import { test, expect } from '@playwright/test';
test('crea tercero, prepara borrador, contabiliza e imprime conservando el documento', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Registrar empresa', exact: true }).click();
  await page.getByLabel('Razón social').fill('Empresa de comprobantes');
  await page.getByLabel('NIT', { exact: false }).fill('000903');
  await page.getByLabel('Dígito de verificación').fill('0');
  await page.getByRole('button', { name: 'Registrar empresa', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Empresa de comprobantes', exact: true }),
  ).toBeVisible();
  const company = new URL(page.url()).pathname.split('/')[2];
  await page.getByRole('link', { name: 'Abrir módulo Contabilidad' }).click();
  await page
    .getByRole('navigation', { name: 'Menú de contabilidad', exact: true })
    .getByRole('link', { name: 'Terceros', exact: true })
    .click();
  await page.getByLabel('Identificación *', { exact: true }).fill('000123');
  await page.getByLabel('Dígito de verificación *', { exact: true }).fill('0');
  await page.getByLabel('Nombre o razón social *', { exact: true }).fill('Proveedor de ejemplo');
  await page.getByRole('button', { name: 'Guardar tercero', exact: true }).click();
  await expect(
    page.getByText('El tercero se guardó en PostgreSQL.', { exact: true }),
  ).toBeVisible();
  await page.getByRole('button', { name: 'Editar tercero 000123', exact: true }).click();
  await page.getByLabel('Nombre o razón social *', { exact: true }).fill('Proveedor actualizado');
  await Promise.all([
    page.waitForResponse(
      (r) => r.request().method() === 'PUT' && r.url().includes('/third-parties/'),
    ),
    page.getByRole('button', { name: 'Guardar tercero', exact: true }).click(),
  ]);
  const parties = await (
    await page.request.get('/api/v1/companies/' + company + '/third-parties')
  ).json();
  const party = parties.items[0];
  const createAccount = async (code: string) => {
    const response = await page.request.post('/api/v1/companies/' + company + '/accounts', {
      data: {
        code,
        name: 'Cuenta ' + code,
        nature: 'DEBIT',
        active: true,
        allowsPosting: true,
        notes: '',
        version: 0,
      },
    });
    expect(response.status()).toBe(201);
    return response.json();
  };
  const debit = await createAccount('11050501'),
    credit = await createAccount('11100501');
  await page.getByRole('link', { name: 'Ir a comprobantes →', exact: true }).click();
  await expect(
    page.getByRole('heading', { name: 'Comprobantes contables', exact: true }),
  ).toBeVisible();
  await page.getByLabel('Fecha *', { exact: true }).fill('2026-11-16');
  await page.getByLabel('Origen de la operación *', { exact: true }).fill('Pago de prueba');
  await page.getByLabel('Elaborado por *', { exact: true }).fill('Responsable de prueba');
  await page
    .getByLabel('Descripción del comprobante *', { exact: true })
    .fill('Comprobante de prueba');
  await page.getByLabel('Referencia al soporte *', { exact: true }).fill('Archivo externo TEST-1');
  await page.getByLabel('Cuenta 1 *', { exact: true }).selectOption(debit.id);
  await page.getByLabel('Tercero 1 *', { exact: true }).selectOption(party.id);
  await page.getByLabel('Débito 1', { exact: true }).fill('100,10');
  await page.getByLabel('Cuenta 2 *', { exact: true }).selectOption(credit.id);
  await page.getByLabel('Tercero 2 *', { exact: true }).selectOption(party.id);
  await page.getByLabel('Crédito 2', { exact: true }).fill('90.00');
  const [draftResponse] = await Promise.all([
    page.waitForResponse((r) => r.request().method() === 'POST' && r.url().endsWith('/journals')),
    page.getByRole('button', { name: 'Guardar borrador', exact: true }).click(),
  ]);
  expect(draftResponse.status()).toBe(201);
  const draft = await draftResponse.json();
  await expect(page.getByRole('button', { name: 'Contabilizar', exact: true })).toBeDisabled();
  await page.getByLabel('Crédito 2', { exact: true }).fill('100.10');
  await Promise.all([
    page.waitForResponse((r) => r.request().method() === 'PUT' && r.url().includes('/journals/')),
    page.getByRole('button', { name: 'Guardar borrador', exact: true }).click(),
  ]);
  await expect(page.getByRole('button', { name: 'Contabilizar', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: 'Contabilizar', exact: true }).click();
  await expect(
    page.getByText('Comprobante contabilizado: CC-2026-000001', { exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel('Origen de la operación *', { exact: true })).toBeDisabled();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: 'test-results/comprobante-contabilizado.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.emulateMedia({ media: 'print' });
  await expect(
    page.getByRole('heading', { name: 'Comprobante contable CC-2026-000001', exact: true }),
  ).toBeVisible();
  await page.pdf({ path: 'test-results/comprobante-contable.pdf', format: 'A4' });
  await page.emulateMedia({ media: 'screen' });
  await page.reload();
  await page.getByRole('button', { name: 'Abrir comprobante ' + draft.id, exact: true }).click();
  await expect(page.getByText('Contabilizado · Solo consulta', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Guardar borrador', exact: true })).toHaveCount(0);
});
