import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { Cart } from '../cart/cart';
import { ShopConnection } from '../connection/shop-connection';
import { Catalog, Offer, Product } from './catalog';
import { iconFor } from './product-icons';

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

  private readonly connection = inject(ShopConnection);

  protected readonly iconFor = iconFor;

  /**
   * What the shop sells, or undefined until the shop has answered. Undefined is what lets the
   * page tell "still loading" apart from "the shop sells nothing". While the shop cannot be
   * reached, the request keeps being sent again instead of failing.
   */
  readonly products = toSignal<Product[]>(
    this.catalog.products().pipe(this.connection.keepTrying()),
  );

  /** This week's offers. None until they arrive; the shelf is usable without them. */
  private readonly offers = toSignal(this.catalog.offers().pipe(this.connection.keepTrying()), {
    initialValue: [] as Offer[],
  });

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
