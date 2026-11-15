import { test, expect } from '@playwright/test';
test('abre directamente sin autenticación y registra una empresa contra PostgreSQL', async ({
  page,
}) => {
  const external: string[] = [];
  page.on('request', (request) => {
    if (
      new URL(request.url()).origin !==
      new URL(process.env['E2E_BASE_URL'] || 'http://localhost:4200').origin
    )
      external.push(request.url());
  });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Mis empresas', exact: true })).toBeVisible();
  await expect(page.getByRole('note')).toContainText('PostgreSQL');
  await expect(page.getByRole('button', { name: 'Iniciar sesión' })).toHaveCount(0);
  await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
  await page.getByLabel('Razón social').fill('Ejemplo local S.A.S.');
  await page.getByLabel('NIT', { exact: false }).fill('000001');
  await page.getByLabel('Dígito de verificación').fill('0');
  await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Ejemplo local S.A.S.', exact: true }),
  ).toBeVisible();
  await page.getByRole('link', { name: 'Volver a mis empresas' }).click();
  await expect(page.getByRole('link', { name: 'Ejemplo local S.A.S.', exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.reload();
  await expect(page.getByRole('link', { name: 'Ejemplo local S.A.S.', exact: true })).toBeVisible();
  expect(external).toEqual([]);
});
test('valida el formulario y evita NIT duplicado', async ({ page }) => {
  await page.goto('/empresas/nueva');
  await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
  await expect(page.getByText('Ingresa una razón social de hasta 200 caracteres.')).toBeVisible();
  for (const name of ['Primera', 'Segunda']) {
    await page.getByLabel('Razón social').fill(name);
    await page.getByLabel('NIT', { exact: false }).fill('0001');
    await page.getByLabel('Dígito de verificación').fill('0');
    await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
    if (name === 'Primera') {
      await page.getByRole('link', { name: 'Volver a mis empresas' }).click();
      await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
    }
  }
  await expect(page.getByRole('alert')).toContainText('Comprueba el NIT');
});
test('muestra un error recuperable al fallar la configuración', async ({ page }) => {
  await page.route('**/config.json', (route) => route.fulfill({ status: 503, body: '{}' }));
  await page.goto('/');
  await expect(page.getByRole('alert')).toContainText('configuración');
  await expect(page.getByRole('button', { name: 'Volver a cargar' })).toBeVisible();
});
