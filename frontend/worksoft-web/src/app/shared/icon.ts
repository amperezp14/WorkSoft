import { Component, input } from '@angular/core';
@Component({
  selector: 'app-icon',
  template: `<svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    @switch (name()) {
      @case ('plus') {
        <path d="M12 5v14M5 12h14" />
      }
      @case ('arrow') {
        <path d="m9 5 7 7-7 7" />
      }
      @case ('search') {
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m16 16 4 4" />
      }
      @case ('logout') {
        <path d="M9 4H4v16h5M12 12h9m-4-4 4 4-4 4" />
      }
      @case ('check') {
        <path d="m5 12 4 4L19 6" />
      }
      @case ('lock') {
        <rect x="5" y="10" width="14" height="11" rx="3" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
      }
      @default {
        <rect x="4" y="3" width="16" height="18" rx="2" />
        <path d="M8 7h2m4 0h2M8 11h2m4 0h2M9 21v-6h6v6" />
      }
    }
  </svg>`,
  styles: [
    `
      :host {
        display: inline-flex;
        width: 1.25rem;
        height: 1.25rem;
        flex: 0 0 auto;
      }
      svg {
        width: 100%;
        height: 100%;
      }
    `,
  ],
})
export class Icon {
  readonly name = input('building');
}
