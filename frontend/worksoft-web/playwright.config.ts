import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  timeout: 60000,
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:4200',
    browserName: 'chromium',
    screenshot: 'only-on-failure',
    trace: 'off',
    launchOptions: { executablePath: process.env['PLAYWRIGHT_CHROMIUM_EXECUTABLE'] },
  },
  webServer: process.env['E2E_BASE_URL']
    ? undefined
    : {
        command: 'npm start -- --host localhost --port 4200',
        url: 'http://localhost:4200',
        reuseExistingServer: !process.env['CI'],
        timeout: 120000,
      },
});
