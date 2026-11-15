import { test, expect } from '@playwright/test';
test('consulta referencia, crea y actualiza una cuenta y conserva los cambios al recargar', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
  await page.getByLabel('Razón social').fill('Empresa PUC de prueba');
  await page.getByLabel('NIT', { exact: false }).fill('000901');
  await page.getByLabel('Dígito de verificación').fill('0');
  await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
  await page.getByRole('link', { name: 'Abrir módulo Contabilidad' }).click();
  await page
    .getByRole('navigation', { name: 'Menú de contabilidad', exact: true })
    .getByRole('link', { name: 'Plan de cuentas', exact: true })
    .click();
  await expect(page.getByRole('heading', { name: 'Plan de cuentas', exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'Buscar cuentas' }).fill('110505');
  await page.getByRole('button', { name: 'Buscar', exact: true }).click();
  await expect(page.getByText('CAJA GENERAL', { exact: true })).toBeVisible();
  await page.getByLabel('Código *', { exact: true }).fill('11050501');
  await page.getByLabel('Nombre *', { exact: true }).fill('Caja principal de prueba');
  await page.getByRole('button', { name: 'Guardar cuenta', exact: true }).click();
  await expect(page.getByText('La cuenta se guardó en PostgreSQL.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Editar cuenta 11050501', exact: true }).click();
  await expect(page.getByLabel('Código *', { exact: true })).toBeDisabled();
  await page.getByLabel('Nombre *', { exact: true }).fill('Caja principal actualizada');
  await page.getByRole('button', { name: 'Guardar cambios', exact: true }).click();
  await expect(page.getByText('Caja principal actualizada', { exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole('textbox', { name: 'Buscar cuentas' }).fill('11050501');
  await page.getByRole('button', { name: 'Buscar', exact: true }).click();
  await expect(page.getByText('Caja principal actualizada', { exact: true })).toBeVisible();
  await page.screenshot({ path: 'test-results/plan-cuentas-postgres.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
