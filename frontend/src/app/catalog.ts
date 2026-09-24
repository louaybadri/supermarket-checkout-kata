import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** A product on the shelf. Prices are whole cents, exactly as the backend sends them. */
export interface Product {
  sku: string;
  name: string;
  unitPriceCents: number;
}

/** A deal running this week. `items` maps a sku to how many of it the deal needs. */
export interface Offer {
  name: string;
  items: Record<string, number>;
  priceCents: number;
}

@Injectable({ providedIn: 'root' })
export class Catalog {
  private readonly http = inject(HttpClient);

  products(): Observable<Product[]> {
    return this.http.get<Product[]>('/api/products');
  }

  offers(): Observable<Offer[]> {
    return this.http.get<Offer[]>('/api/offers');
  }
}
