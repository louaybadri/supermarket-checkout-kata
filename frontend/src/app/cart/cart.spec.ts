import { TestBed } from '@angular/core/testing';

import { Cart } from './cart';
import { Product } from '../catalog/catalog';

const APPLE: Product = { sku: 'APPLE', name: 'Apple', unitPriceCents: 30 };
const BANANA: Product = { sku: 'BANANA', name: 'Banana', unitPriceCents: 20 };

describe('Cart', () => {
  let cart: Cart;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    cart = TestBed.inject(Cart);
    cart.clear();
  });

  it('starts empty', () => {
    expect(cart.isEmpty()).toBe(true);
    expect(cart.itemCount()).toBe(0);
  });

  it('opens a line for a product picked up for the first time', () => {
    cart.add(APPLE);

    expect(cart.lines()).toEqual([{ product: APPLE, quantity: 1 }]);
    expect(cart.itemCount()).toBe(1);
  });

  it('counts the same product on one line', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    cart.add(BANANA);

    expect(cart.lines()).toEqual([
      { product: APPLE, quantity: 2 },
      { product: BANANA, quantity: 1 },
    ]);
    expect(cart.itemCount()).toBe(3);
  });

  it('puts one back without losing the line', () => {
    cart.add(APPLE);
    cart.add(APPLE);

    cart.removeOne('APPLE');

    expect(cart.lines()).toEqual([{ product: APPLE, quantity: 1 }]);
  });

  it('drops the line when the last one is put back', () => {
    cart.add(APPLE);

    cart.removeOne('APPLE');

    expect(cart.isEmpty()).toBe(true);
  });

  it('takes a whole line out at once', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    cart.add(BANANA);

    cart.removeLine('APPLE');

    expect(cart.lines()).toEqual([{ product: BANANA, quantity: 1 }]);
  });

  it('hands the checkout a sku and a quantity per line', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    cart.add(BANANA);

    expect(cart.toRequestItems()).toEqual([
      { sku: 'APPLE', quantity: 2 },
      { sku: 'BANANA', quantity: 1 },
    ]);
  });
});
