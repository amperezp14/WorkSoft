import { test, expect, Page } from '@playwright/test';
async function register(page: Page, name: string, nit: string): Promise<string> {
  await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
  await page.getByLabel('Razón social').fill(name);
  await page.getByLabel('NIT', { exact: false }).fill(nit);
  await page.getByLabel('Dígito de verificación').fill('0');
  await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  return new URL(page.url()).pathname.split('/')[2];
}
test('selecciona empresa y navega por contabilidad, facturación y datos', async ({ page }) => {
  await page.goto('/');
  const id = await register(page, 'Empresa contable de ejemplo', '000100');
  await expect(page.getByRole('heading', { name: '¿En qué quieres trabajar?' })).toBeVisible();
  await page.getByRole('link', { name: 'Abrir módulo Contabilidad' }).click();
  await expect(page).toHaveURL(new RegExp('/empresas/' + id + '/contabilidad$'));
  const accounting = page.getByRole('navigation', { name: 'Menú de contabilidad', exact: true });
  for (const item of [
    'Plan de cuentas',
    'Comprobantes contables',
    'Terceros',
    'Informes contables',
  ])
    await expect(accounting.getByRole('link', { name: item, exact: true })).toBeVisible();
  await accounting.getByRole('link', { name: 'Plan de cuentas', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Plan de cuentas', exact: true })).toBeVisible();
  await expect(
    page.getByRole('heading', { name: 'Empresa contable de ejemplo', exact: true }),
  ).toBeVisible();
  await page.screenshot({ path: 'test-results/contabilidad-desktop.png', fullPage: true });
  const tabs = page.getByRole('navigation', { name: 'Secciones de la empresa', exact: true });
  await tabs.getByRole('link', { name: 'Facturación', exact: true }).click();
  const billing = page.getByRole('navigation', { name: 'Menú de facturación', exact: true });
  await billing.getByRole('link', { name: 'Facturas de venta', exact: true }).click();
  await expect(page).toHaveURL(new RegExp('/empresas/' + id + '/facturacion/facturas-de-venta$'));
  await expect(page.getByRole('heading', { name: 'Facturas de venta', exact: true })).toBeVisible();
  await tabs.getByRole('link', { name: 'Datos de la empresa', exact: true }).click();
  await expect(page.getByText('000100-0', { exact: true })).toBeVisible();
  await tabs.getByRole('link', { name: 'Módulos', exact: true }).click();
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('link', { name: 'Abrir módulo Facturación' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: 'test-results/modulos-mobile.png', fullPage: true });
});
test('cambia el contexto al elegir otra empresa y lo limpia al volver al listado', async ({
  page,
}) => {
  await page.goto('/');
  const first = await register(page, 'Empresa primera', '000201');
  await page.getByRole('link', { name: 'Cambiar empresa', exact: true }).click();
  const second = await register(page, 'Empresa segunda', '000202');
  expect(first).not.toBe(second);
  await page.getByRole('link', { name: 'Abrir módulo Contabilidad' }).click();
  for (const href of await page
    .getByRole('navigation', { name: 'Menú de contabilidad', exact: true })
    .locator('a')
    .evaluateAll((links) => links.map((a) => a.getAttribute('href')))) {
    expect(href).toContain(second);
    expect(href).not.toContain(first);
  }
  await expect(page.getByRole('heading', { name: 'Empresa segunda', exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Cambiar empresa', exact: true }).click();
  await expect(
    page.getByRole('navigation', { name: 'Módulos de la empresa', exact: true }),
  ).toHaveCount(0);
  await page.getByRole('link', { name: 'Empresa primera', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Empresa primera', exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Abrir módulo Facturación' })).toHaveAttribute(
    'href',
    '/empresas/' + first + '/facturacion',
  );
});
test('no muestra módulos de una empresa inexistente o perdida al recargar', async ({ page }) => {
  await page.goto('/empresas/00000000-0000-4000-8000-000000000099/contabilidad');
  await expect(
    page.getByRole('heading', { name: 'Empresa no disponible', exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole('navigation', { name: 'Menú de contabilidad', exact: true }),
  ).toHaveCount(0);
  await expect(
    page.getByRole('navigation', { name: 'Módulos de la empresa', exact: true }),
  ).toHaveCount(0);
});
