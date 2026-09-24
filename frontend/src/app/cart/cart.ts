import { computed, Injectable, signal } from '@angular/core';

import { Product } from '../catalog/catalog';

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

  /**
   * Adds one more of a product, or starts a line for it. Nothing happens once the cart holds as
   * many as the shop allows.
   */
  add(product: Product): void {
    if (!this.canAddMoreOf(product)) {
      return;
    }
    this.contents.update((lines) =>
      lines.some((line) => line.product.sku === product.sku)
        ? lines.map((line) =>
            line.product.sku === product.sku ? { ...line, quantity: line.quantity + 1 } : line,
          )
        : [...lines, { product, quantity: 1 }],
    );
  }

  /**
   * Whether one more of this product may go in. The limit is not written here: it arrives with
   * the product from the backend, which enforces the same number at checkout.
   */
  canAddMoreOf(product: Product): boolean {
    return this.quantityOf(product.sku) < product.maxQuantity;
  }

  /** How many of a product the cart holds, 0 if it has none. */
  quantityOf(sku: string): number {
    return this.contents().find((line) => line.product.sku === sku)?.quantity ?? 0;
  }

  /**
   * Sets a line to the number the shopper typed, kept between 1 and the product's limit, so 150
   * becomes the limit and 0 becomes 1. Taking a line out is what the minus and bin buttons are
   * for; a line never vanishes because its field was cleared while typing.
   *
   * Anything that is not a whole number is ignored, and so is a product not in the cart.
   */
  setQuantity(sku: string, quantity: number): void {
    if (!Number.isInteger(quantity)) {
      return;
    }
    this.contents.update((lines) =>
      lines.map((line) =>
        line.product.sku === sku
          ? { ...line, quantity: Math.min(Math.max(quantity, 1), line.product.maxQuantity) }
          : line,
      ),
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
