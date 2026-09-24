import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductList } from './product-list';

describe('ProductList', () => {
  let fixture: ComponentFixture<ProductList>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function answerWith(
    products: { sku: string; name: string; unitPriceCents: number }[],
    offers: { name: string; items: Record<string, number>; priceCents: number }[],
  ) {
    http.expectOne('/api/products').flush(products);
    http.expectOne('/api/offers').flush(offers);
    fixture.detectChanges();
  }

  it('lists what the shop sells, with prices in euros', () => {
    answerWith(
      [
        { sku: 'APPLE', name: 'Apple', unitPriceCents: 30 },
        { sku: 'MILK', name: 'Milk', unitPriceCents: 90 },
      ],
      [],
    );

    const page = fixture.nativeElement as HTMLElement;
    const names = Array.from(page.querySelectorAll('.name')).map((node) => node.textContent?.trim());
    const prices = Array.from(page.querySelectorAll('.price')).map((node) => node.textContent?.trim());

    expect(names).toEqual(['Apple', 'Milk']);
    expect(prices[0]).toContain('0.30');
    expect(prices[1]).toContain('0.90');
  });

  it('shows an offer next to every product it mentions', () => {
    answerWith(
      [
        { sku: 'APPLE', name: 'Apple', unitPriceCents: 30 },
        { sku: 'BANANA', name: 'Banana', unitPriceCents: 20 },
      ],
      [{ name: 'Apple & banana for 0.40', items: { APPLE: 1, BANANA: 1 }, priceCents: 40 }],
    );

    const page = fixture.nativeElement as HTMLElement;
    const badges = Array.from(page.querySelectorAll('.offer')).map((node) => node.textContent?.trim());

    expect(badges).toEqual(['Apple & banana for 0.40', 'Apple & banana for 0.40']);
  });

  it('says so when the shop is empty', () => {
    answerWith([], []);

    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('The shop is empty.');
  });
});
