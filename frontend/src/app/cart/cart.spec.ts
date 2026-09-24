import { TestBed } from '@angular/core/testing';

import { Cart } from './cart';
import { Product } from '../catalog/catalog';

const APPLE: Product = { sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 99 };
const BANANA: Product = { sku: 'BANANA', name: 'Banana', unitPriceCents: 20, maxQuantity: 99 };
/** A product the shop only lets a cart hold two of, so the limit is quick to reach. */
const CHEESE: Product = { sku: 'CHEESE', name: 'Cheese', unitPriceCents: 250, maxQuantity: 2 };

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

  it('stops at the most the shop allows of one product', () => {
    cart.add(CHEESE);
    cart.add(CHEESE);
    cart.add(CHEESE);

    expect(cart.lines()).toEqual([{ product: CHEESE, quantity: 2 }]);
    expect(cart.canAddMoreOf(CHEESE)).toBe(false);
  });

  it('allows more again once one is put back', () => {
    cart.add(CHEESE);
    cart.add(CHEESE);

    cart.removeOne('CHEESE');

    expect(cart.canAddMoreOf(CHEESE)).toBe(true);
  });

  it('sets a line to the quantity typed', () => {
    cart.add(APPLE);

    cart.setQuantity('APPLE', 12);

    expect(cart.quantityOf('APPLE')).toBe(12);
  });

  it('never goes past the limit when a number is typed', () => {
    cart.add(CHEESE);

    cart.setQuantity('CHEESE', 150);

    expect(cart.quantityOf('CHEESE')).toBe(2);
  });

  it('keeps at least one when zero or less is typed', () => {
    cart.add(APPLE);
    cart.add(APPLE);

    cart.setQuantity('APPLE', 0);
    expect(cart.quantityOf('APPLE')).toBe(1);

    cart.setQuantity('APPLE', -3);
    expect(cart.quantityOf('APPLE')).toBe(1);
  });

  it('ignores something that is not a whole number', () => {
    cart.add(APPLE);
    cart.add(APPLE);

    cart.setQuantity('APPLE', Number.NaN);
    cart.setQuantity('APPLE', 2.5);

    expect(cart.quantityOf('APPLE')).toBe(2);
  });

  it('leaves a product that is not in the cart alone', () => {
    cart.add(APPLE);

    cart.setQuantity('BANANA', 3);

    expect(cart.lines()).toEqual([{ product: APPLE, quantity: 1 }]);
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
