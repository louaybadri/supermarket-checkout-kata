import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cart } from '../cart/cart';
import { Product } from './catalog';
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
    products: Product[],
    offers: { name: string; items: Record<string, number>; priceCents: number }[],
  ) {
    http.expectOne('/api/products').flush(products);
    http.expectOne('/api/offers').flush(offers);
    fixture.detectChanges();
  }

  it('lists what the shop sells, with prices in euros', () => {
    answerWith(
      [
        { sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 99 },
        { sku: 'MILK', name: 'Milk', unitPriceCents: 90, maxQuantity: 99 },
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
        { sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 99 },
        { sku: 'BANANA', name: 'Banana', unitPriceCents: 20, maxQuantity: 99 },
      ],
      [{ name: 'Apple & banana for 0.40', items: { APPLE: 1, BANANA: 1 }, priceCents: 40 }],
    );

    const page = fixture.nativeElement as HTMLElement;
    const badges = Array.from(page.querySelectorAll('.offer')).map((node) => node.textContent?.trim());

    expect(badges).toEqual(['Apple & banana for 0.40', 'Apple & banana for 0.40']);
  });

  it('turns the add button off once the cart holds the most the shop allows', () => {
    const cheese: Product = { sku: 'CHEESE', name: 'Cheese', unitPriceCents: 250, maxQuantity: 1 };
    const cart = TestBed.inject(Cart);
    cart.clear();
    answerWith([cheese], []);
    const add = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.add')!;
    expect(add.disabled).toBe(false);

    cart.add(cheese);
    fixture.detectChanges();

    expect(add.disabled).toBe(true);
  });

  it('says it is still loading, not that the shop is empty, while the shop cannot be reached', () => {
    vi.useFakeTimers();
    const shopDown = { status: 502, statusText: 'Bad Gateway' };
    http.expectOne('/api/products').flush(null, shopDown);
    http.expectOne('/api/offers').flush(null, shopDown);
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Loading the shelf…');
    expect(page.textContent).not.toContain('The shop is empty.');

    // Three seconds later both are asked for again, and this time the shop answers.
    vi.advanceTimersByTime(3000);
    answerWith([{ sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 99 }], []);

    expect(page.querySelector('.name')?.textContent).toContain('Apple');
    vi.useRealTimers();
  });

  it('says so when the shop is empty', () => {
    answerWith([], []);

    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('The shop is empty.');
  });
});
