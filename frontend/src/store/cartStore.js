import { create } from 'zustand';
import { cartApi } from '../services/api';
import toast from 'react-hot-toast';

const normalizeCartItem = (item) => {
  const price = item.currentPrice ?? item.price ?? item.variant?.price ?? item.product?.salePrice ?? item.product?.basePrice ?? 0;

  return {
    id: item.variantId ?? item.id,
    productId: item.productId,
    product: {
      id: item.productId,
      name: item.productName || item.product?.name || '',
      slug: item.productSlug || item.product?.slug || '',
      images: item.productImage
        ? [{ url: item.productImage }]
        : item.product?.images || [],
      salePrice: item.salePrice,
      basePrice: item.price,
    },
    variantId: item.variantId,
    variant: {
      id: item.variantId,
      name: item.variantName || item.variant?.name || '',
      price: item.currentPrice ?? item.price,
    },
    quantity: item.quantity ?? 0,
    price,
    subtotal: item.subtotal,
    stockQuantity: item.stockQuantity,
    inStock: item.inStock,
    createdAt: item.createdAt,
  };
};

const normalizeCartResponse = (data) => {
  if (!data || !Array.isArray(data.items)) {
    return [];
  }

  return data.items.map(normalizeCartItem);
};

export const useCartStore = create((set, get) => ({
  items: [],
  isLoading: false,

  fetchCart: async () => {
    set({ isLoading: true });
    try {
      const response = await cartApi.get();
      set({ items: normalizeCartResponse(response.data), isLoading: false });
      return response.data;
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  addToCart: async (product, variant, quantity = 1) => {
    try {
      const response = await cartApi.add({
        productId: product.id,
        variantId: variant?.id,
        quantity,
      });

      set({ items: normalizeCartResponse(response.data) });
      toast.success('Đã thêm vào giỏ hàng!');
      return response.data;
    } catch (error) {
      const message = error.response?.data?.message || 'Không thể thêm vào giỏ hàng';
      toast.error(message);
      throw error;
    }
  },

  updateQuantity: async (itemId, quantity) => {
    if (quantity < 1) return;

    try {
      const response = await cartApi.update(itemId, quantity);
      set({ items: normalizeCartResponse(response.data) });
      return response.data;
    } catch (error) {
      toast.error('Không thể cập nhật số lượng');
      throw error;
    }
  },

  removeFromCart: async (itemId) => {
    try {
      const response = await cartApi.remove(itemId);
      set({ items: normalizeCartResponse(response.data) });
      toast.success('Đã xóa khỏi giỏ hàng');
      return response.data;
    } catch (error) {
      toast.error('Không thể xóa sản phẩm');
      throw error;
    }
  },

  clearCart: async () => {
    try {
      await cartApi.clear();
      set({ items: [] });
      return true;
    } catch (error) {
      set({ items: [] });
      throw error;
    }
  },

  addItemLocal: (product, variant, quantity = 1) => get().addToCart(product, variant, quantity),
  updateQuantityLocal: (itemId, quantity) => get().updateQuantity(itemId, quantity),
  removeItemLocal: (itemId) => get().removeFromCart(itemId),
  clearCartLocal: () => get().clearCart(),

  get totalItems() {
    return get().items.reduce((total, item) => total + item.quantity, 0);
  },

  get totalPrice() {
    return get().items.reduce((total, item) => {
      const price = item.price || item.variant?.price || item.product?.salePrice || item.product?.basePrice || 0;
      return total + price * item.quantity;
    }, 0);
  },

  getItemCount: () => {
    return get().items.reduce((total, item) => total + item.quantity, 0);
  },

  getTotalPrice: () => {
    return get().items.reduce((total, item) => {
      const price = item.price || item.variant?.price || item.product?.salePrice || item.product?.basePrice || 0;
      return total + price * item.quantity;
    }, 0);
  },
}));
