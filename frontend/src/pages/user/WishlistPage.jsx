import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiHeart, FiShoppingCart, FiTrash2, FiStar } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { wishlistApi, cartApi } from '../../services/api';

const WishlistPage = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState(null);

  useEffect(() => {
    fetchWishlist();
  }, []);

  const fetchWishlist = async () => {
    try {
      const response = await wishlistApi.getAll();
      setProducts(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Error fetching wishlist:', error);
      toast.error('Không thể tải danh sách yêu thích');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (productId) => {
    setRemovingId(productId);
    try {
      await wishlistApi.remove(productId);
      setProducts(prev => prev.filter(p => p.id !== productId));
      toast.success('Đã xóa khỏi danh sách yêu thích');
    } catch (error) {
      toast.error('Không thể xóa sản phẩm');
    } finally {
      setRemovingId(null);
    }
  };

  const formatPrice = (price) => {
    if (!price) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-petshop-orange border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-800">Sản phẩm yêu thích</h1>
        <span className="text-gray-500">{products.length} sản phẩm</span>
      </div>

      {products.length === 0 ? (
        <div className="bg-white rounded-2xl p-12 text-center">
          <FiHeart className="text-6xl text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-800 mb-2">Chưa có sản phẩm yêu thích</h3>
          <p className="text-gray-500 mb-6">Hãy thêm sản phẩm vào danh sách yêu thích để theo dõi</p>
          <Link to="/products" className="btn-primary">
            Khám phá sản phẩm
          </Link>
        </div>
      ) : (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product, index) => (
            <motion.div
              key={product.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              className="bg-white rounded-2xl shadow-sm overflow-hidden group"
            >
              {/* Product Image */}
              <Link to={`/products/${product.slug}`} className="block relative h-48 overflow-hidden">
                {product.primaryImage ? (
                  <img
                    src={product.primaryImage}
                    alt={product.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <div className="w-full h-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                    <FiShoppingCart className="text-4xl text-gray-300" />
                  </div>
                )}
                
                {/* Remove Button */}
                <button
                  onClick={(e) => {
                    e.preventDefault();
                    handleRemove(product.id);
                  }}
                  disabled={removingId === product.id}
                  className="absolute top-3 right-3 w-9 h-9 bg-white rounded-full flex items-center justify-center shadow-md hover:bg-red-50 transition-colors"
                >
                  {removingId === product.id ? (
                    <div className="animate-spin rounded-full h-4 w-4 border-2 border-red-500 border-t-transparent"></div>
                  ) : (
                    <FiTrash2 className="text-red-500" />
                  )}
                </button>

                {/* Out of Stock Badge */}
                {!product.inStock && (
                  <div className="absolute bottom-3 left-3 px-3 py-1 bg-red-500 text-white text-xs font-medium rounded-full">
                    Hết hàng
                  </div>
                )}
              </Link>

              {/* Product Info */}
              <div className="p-4">
                <Link to={`/products/${product.slug}`}>
                  <h3 className="font-medium text-gray-800 mb-2 line-clamp-2 hover:text-petshop-orange transition-colors">
                    {product.name}
                  </h3>
                </Link>

                {/* Rating */}
                {product.averageRating > 0 && (
                  <div className="flex items-center gap-1 mb-2">
                    <FiStar className="text-yellow-400 fill-yellow-400 text-sm" />
                    <span className="text-sm text-gray-600">
                      {product.averageRating?.toFixed(1)} ({product.reviewCount})
                    </span>
                  </div>
                )}

                {/* Price */}
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-lg font-bold text-petshop-orange">
                      {formatPrice(product.minPrice || product.basePrice)}
                    </p>
                    {product.maxPrice && product.minPrice !== product.maxPrice && (
                      <p className="text-xs text-gray-400">
                        ~ {formatPrice(product.maxPrice)}
                      </p>
                    )}
                  </div>
                  {product.soldCount > 0 && (
                    <span className="text-xs text-gray-400">
                      Đã bán {product.soldCount}
                    </span>
                  )}
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
};

export default WishlistPage;
