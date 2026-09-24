import { computed, Injectable, signal } from '@angular/core';

import { Product } from './catalog';

export interface CartLine {
  product: Product;
  quantity: number;
}

/**
 * What the shopper has picked up. It holds items and nothing else: no prices are worked out
 * here, because the backend is the only place that knows what a cart costs.
 */
@Injectable({ providedIn: 'root' })
export class Cart {
  private readonly contents = signal<CartLine[]>([]);

  readonly lines = this.contents.asReadonly();

  readonly itemCount = computed(() => this.contents().reduce((count, line) => count + line.quantity, 0));

  readonly isEmpty = computed(() => this.contents().length === 0);

  /** Adds one more of a product, or starts a line for it. */
  add(product: Product): void {
    this.contents.update((lines) =>
      lines.some((line) => line.product.sku === product.sku)
        ? lines.map((line) =>
            line.product.sku === product.sku ? { ...line, quantity: line.quantity + 1 } : line,
          )
        : [...lines, { product, quantity: 1 }],
    );
  }

  /** Puts one back. The line disappears when the last one goes. */
  removeOne(sku: string): void {
    this.contents.update((lines) =>
      lines
        .map((line) => (line.product.sku === sku ? { ...line, quantity: line.quantity - 1 } : line))
        .filter((line) => line.quantity > 0),
    );
  }

  /** Takes the whole line out, however many there were. */
  removeLine(sku: string): void {
    this.contents.update((lines) => lines.filter((line) => line.product.sku !== sku));
  }

  clear(): void {
    this.contents.set([]);
  }

  /** The cart as the checkout endpoint wants it. */
  toRequestItems(): { sku: string; quantity: number }[] {
    return this.contents().map((line) => ({ sku: line.product.sku, quantity: line.quantity }));
  }
}
