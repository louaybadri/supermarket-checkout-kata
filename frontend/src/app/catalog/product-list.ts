import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { Cart } from '../cart/cart';
import { Catalog, Offer, Product } from './catalog';

/** The shelf: what is for sale, and which deals mention each product. */
@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductList {
  private readonly catalog = inject(Catalog);

  protected readonly cart = inject(Cart);

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
