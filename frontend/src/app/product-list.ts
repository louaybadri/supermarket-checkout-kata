import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { Catalog, Offer, Product } from './catalog';

/** The shelf: what is for sale, and which deals mention each product. */
@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe],
  template: `
    <section class="shelf">
      <h2>On the shelf</h2>

      @for (product of products(); track product.sku) {
        <article class="product">
          <span class="name">{{ product.name }}</span>
          <span class="price">{{ product.unitPriceCents / 100 | currency: 'EUR' }}</span>

          @for (offer of offersFor()[product.sku] ?? []; track offer.name) {
            <span class="offer">{{ offer.name }}</span>
          }
        </article>
      } @empty {
        <p>The shop is empty.</p>
      }
    </section>
  `,
  styles: `
    .product {
      display: flex;
      align-items: baseline;
      gap: 0.75rem;
      padding: 0.35rem 0;
    }
    .name {
      min-width: 6rem;
      font-weight: 600;
    }
    .offer {
      font-size: 0.8rem;
      padding: 0.1rem 0.45rem;
      border-radius: 999px;
      background: #eef6ee;
      color: #256029;
    }
  `,
})
export class ProductList {
  private readonly catalog = inject(Catalog);

  readonly products = toSignal(this.catalog.products(), { initialValue: [] as Product[] });

  private readonly offers = toSignal(this.catalog.offers(), { initialValue: [] as Offer[] });

  /** Offers grouped by the products they mention, so each one can be shown next to its shelf line. */
  readonly offersFor = computed(() => {
    const bySku: Record<string, Offer[]> = {};
    for (const offer of this.offers()) {
      for (const sku of Object.keys(offer.items)) {
        (bySku[sku] ??= []).push(offer);
      }
    }
    return bySku;
  });
}
