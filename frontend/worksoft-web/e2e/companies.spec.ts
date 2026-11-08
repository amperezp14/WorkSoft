import { test, expect, Page } from '@playwright/test';
async function runtime(page: Page): Promise<void> {
  const url = process.env['E2E_KEYCLOAK_URL'];
  if (url)
    await page.route('**/config.json', (route) =>
      route.fulfill({
        json: {
          apiBaseUrl: '/api/v1',
          keycloak: { url, realm: 'worksoft-local', clientId: 'worksoft-web' },
        },
      }),
    );
}
async function login(page: Page, reader = false): Promise<void> {
  await runtime(page);
  await page.goto('/');
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await page
    .locator('#username')
    .fill(process.env[reader ? 'E2E_READER_USERNAME' : 'E2E_USERNAME']!);
  await page
    .locator('#password')
    .fill(process.env[reader ? 'E2E_READER_PASSWORD' : 'E2E_PASSWORD']!);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { name: 'Mis empresas', exact: true })).toBeVisible();
}
async function fillCompany(page: Page, name: string, nit: string): Promise<void> {
  await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
  await page.getByLabel('Razón social').fill(name);
  await page.getByLabel('NIT', { exact: false }).fill(nit);
  await page.getByLabel('Dígito de verificación').fill('0');
}
function uniqueNit(): string {
  return '00' + String(Date.now()).slice(-12);
}

test('inicio accesible sin sesión y diseño móvil', async ({ page }) => {
  await runtime(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Iniciar sesión', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Tu espacio de trabajo' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('error de configuración permite reintentar', async ({ page }) => {
  await page.route('**/config.json', (route) => route.fulfill({ status: 503, body: '{}' }));
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('servicio de acceso');
  await expect(page.getByRole('button', { name: 'Volver a intentar' })).toBeVisible();
});

test.describe('Operador autenticado con Keycloak real', () => {
  test.beforeEach(async ({ page }) => login(page));
  test('registra, consulta y busca una empresa conservando ceros del NIT', async ({ page }) => {
    const nit = uniqueNit();
    const name = 'Empresa sintética ' + nit;
    await fillCompany(page, name, nit);
    await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('registró correctamente');
    await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
    await expect(page.getByText(nit + '-0', { exact: true })).toBeVisible();
    await page.getByRole('link', { name: 'Volver a mis empresas' }).click();
    await page.getByRole('searchbox').fill(nit);
    await expect(page.getByRole('link', { name, exact: true })).toBeVisible();
    await page.screenshot({ path: 'test-results/empresas-desktop.png', fullPage: true });
    await page.setViewportSize({ width: 390, height: 844 });
    const companyLink = page.getByRole('link', { name, exact: true });
    expect((await companyLink.boundingBox())!.height).toBeLessThan(100);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
    await page.screenshot({ path: 'test-results/empresas-mobile.png', fullPage: true });
  });
  test('valida datos antes de enviar el registro', async ({ page }) => {
    await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
    await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
    await expect(page.getByText('Ingresa una razón social de hasta 200 caracteres.')).toBeVisible();
    await expect(page.getByText('Ingresa entre 1 y 15 dígitos, sin separadores.')).toBeVisible();
    await expect(page.getByText('Ingresa un dígito del 0 al 9.')).toBeVisible();
  });
  test('recupera una respuesta perdida reutilizando la clave sin duplicar empresa', async ({
    page,
  }) => {
    let originalId = '';
    const keys: string[] = [];
    await page.route('**/api/v1/companies', async (route) => {
      if (route.request().method() !== 'POST') {
        await route.continue();
        return;
      }
      keys.push(route.request().headers()['idempotency-key']);
      if (keys.length === 1) {
        const response = await route.fetch();
        expect(response.status()).toBe(201);
        originalId = (await response.json()).id;
        await route.fulfill({ status: 503, json: { code: 'TEMPORARILY_UNAVAILABLE' } });
      } else {
        await route.continue();
      }
    });
    await fillCompany(page, 'Empresa de reintento ' + Date.now(), uniqueNit());
    await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('servicio no está disponible');
    await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
    await expect(page).toHaveURL(new RegExp('/empresas/' + originalId + '$'));
    expect(keys).toHaveLength(2);
    expect(keys[0]).toBe(keys[1]);
  });
  test('muestra un error seguro para una empresa sin acceso', async ({ page }) => {
    await page.goto('/empresas/00000000-0000-4000-8000-000000000099');
    await expect(page.getByRole('heading', { name: 'Empresa no disponible' })).toBeVisible();
    await expect(page.getByRole('alert')).toContainText('no tienes acceso');
  });
});

test('usuario sin permiso no ve registro y no entra por URL directa', async ({ page }) => {
  await login(page, true);
  await expect(page.getByRole('link', { name: 'Registrar empresa', exact: true })).toHaveCount(0);
  await page.goto('/empresas/nueva');
  await expect(page.getByRole('heading', { name: 'Mis empresas', exact: true })).toBeVisible();
  await expect(page.getByText('Aún no tienes empresas asignadas.', { exact: false })).toBeVisible();
});
