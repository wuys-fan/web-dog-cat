import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { FiPlus, FiEdit2, FiTrash2, FiEye, FiEyeOff, FiClock, FiDollarSign } from 'react-icons/fi';
import { MdPets } from 'react-icons/md';
import toast from 'react-hot-toast';
import { servicesApi } from '../../services/api';

const AdminServicesPage = () => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    description: '',
    duration: 60,
    image: '',
    petType: null,
    pricings: [],
  });

  useEffect(() => {
    fetchServices();
  }, []);

  const fetchServices = async () => {
    try {
      const response = await servicesApi.getAll();
      const data = Array.isArray(response.data) ? response.data : response.data?.content || [];
      setServices(data);
    } catch (error) {
      console.error('Error fetching services:', error);
      toast.error('Không thể tải danh sách dịch vụ');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePetTypeChange = (type) => {
    setFormData(prev => ({
      ...prev,
      petType: prev.petType === type ? null : type,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      if (editingService) {
        await servicesApi.update(editingService.id, formData);
        toast.success('Cập nhật dịch vụ thành công!');
      } else {
        await servicesApi.create(formData);
        toast.success('Thêm dịch vụ thành công!');
      }
      fetchServices();
      setShowModal(false);
      resetForm();
    } catch (error) {
      toast.error('Có lỗi xảy ra');
    }
  };

  const handleEdit = (service) => {
    setEditingService(service);
    setFormData({
      name: service.name,
      slug: service.slug,
      description: service.description,
      duration: service.duration,
      image: service.imageUrl || '',
      petType: service.petType || null,
      pricings: service.pricingList || [],
    });
    setShowModal(true);
  };

  const handleDelete = async (serviceId) => {
    if (window.confirm('Bạn có chắc muốn xóa dịch vụ này?')) {
      try {
        await servicesApi.delete(serviceId);
        toast.success('Đã xóa dịch vụ');
        fetchServices();
      } catch (error) {
        toast.error('Không thể xóa dịch vụ');
      }
    }
  };

  const handleToggleStatus = async (service) => {
    try {
      const newActive = !service.active;
      await servicesApi.update(service.id, { ...service, active: newActive });
      toast.success(`Đã ${newActive ? 'kích hoạt' : 'ẩn'} dịch vụ`);
      fetchServices();
    } catch (error) {
      toast.error('Không thể cập nhật trạng thái');
    }
  };

  const resetForm = () => {
    setEditingService(null);
    setFormData({
      name: '',
      slug: '',
      description: '',
      duration: 60,
      image: '',
      petType: null,
      pricings: [],
    });
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  const petTypeOptions = [
    { value: 'DOG', label: 'Chó' },
    { value: 'CAT', label: 'Mèo' },
    { value: 'BIRD', label: 'Chim' },
    { value: 'HAMSTER', label: 'Hamster' },
    { value: 'RABBIT', label: 'Thỏ' },
  ];

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
        <h1 className="text-2xl font-bold text-gray-800">Quản lý dịch vụ</h1>
        <button
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="btn-primary flex items-center gap-2"
        >
          <FiPlus /> Thêm dịch vụ
        </button>
      </div>

      {/* Services Grid */}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {services.map((service, index) => (
          <motion.div
            key={service.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: index * 0.1 }}
            className={`bg-white rounded-2xl shadow-sm overflow-hidden ${service.status === 'INACTIVE' ? 'opacity-60' : ''}`}
          >
            <div className="relative h-40">
              <img
                src={service.imageUrl}
                alt={service.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute top-3 right-3 flex gap-2">
                <button
                  onClick={() => handleToggleStatus(service)}
                  className={`w-8 h-8 rounded-full flex items-center justify-center shadow-md ${
                    service.active ? 'bg-green-500 text-white' : 'bg-gray-400 text-white'
                  }`}
                >
                  {service.active ? <FiEye /> : <FiEyeOff />}
                </button>
              </div>
              {!service.active && (
                <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                  <span className="bg-gray-800 text-white px-3 py-1 rounded-full text-sm">Đã ẩn</span>
                </div>
              )}
            </div>
            <div className="p-4">
              <h3 className="text-lg font-bold text-gray-800 mb-2">{service.name}</h3>
              <p className="text-sm text-gray-500 mb-3 line-clamp-2">{service.description}</p>

              <div className="flex items-center gap-4 mb-3 text-sm">
                <div className="flex items-center gap-1 text-gray-600">
                  <FiClock className="text-petshop-green" />
                  {service.duration} phút
                </div>
                <div className="flex items-center gap-1 text-petshop-orange font-medium">
                  <FiDollarSign />
                  Từ {formatPrice(service.minPrice)}
                </div>
              </div>

              <div className="flex flex-wrap gap-1 mb-4">
                {service.petType && (
                  <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded-full text-xs">
                    {petTypeOptions.find(p => p.value === service.petType)?.label || service.petType}
                  </span>
                )}
              </div>

              <div className="flex items-center justify-between pt-3 border-t">
                <span className="text-sm text-gray-500">Dịch vụ SPA</span>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleEdit(service)}
                    className="p-2 text-gray-500 hover:text-petshop-orange hover:bg-orange-50 rounded-lg"
                  >
                    <FiEdit2 />
                  </button>
                  <button
                    onClick={() => handleDelete(service.id)}
                    className="p-2 text-gray-500 hover:text-red-500 hover:bg-red-50 rounded-lg"
                  >
                    <FiTrash2 />
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-2xl p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto"
          >
            <h2 className="text-xl font-bold text-gray-800 mb-6">
              {editingService ? 'Chỉnh sửa dịch vụ' : 'Thêm dịch vụ mới'}
            </h2>
            
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Tên dịch vụ *
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  className="input-field"
                  placeholder="VD: Tắm và Vệ sinh"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Mô tả
                </label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  rows="3"
                  className="input-field"
                  placeholder="Mô tả chi tiết dịch vụ..."
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Thời gian (phút)
                  </label>
                  <input
                    type="number"
                    name="duration"
                    value={formData.duration}
                    onChange={handleChange}
                    className="input-field"
                    min="15"
                    step="15"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Giá cơ bản (VNĐ)
                  </label>
                  <input
                    type="number"
                    name="basePrice"
                    value={formData.basePrice}
                    onChange={handleChange}
                    className="input-field"
                    placeholder="150000"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  URL hình ảnh
                </label>
                <input
                  type="url"
                  name="image"
                  value={formData.image}
                  onChange={handleChange}
                  className="input-field"
                  placeholder="https://..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Bảng giá theo cân nặng
                </label>
                <div className="text-sm text-gray-600 mb-2">
                  {formData.pricings?.length || 0} mục giá
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Loại thú cưng áp dụng
                </label>
                <div className="flex flex-wrap gap-2">
                  {petTypeOptions.map(type => (
                    <label
                      key={type.value}
                      className={`flex items-center gap-2 px-3 py-2 rounded-lg cursor-pointer border-2 transition-colors ${
                        formData.petType === type.value
                          ? 'border-petshop-orange bg-petshop-orange/10 text-petshop-orange'
                          : 'border-gray-200 text-gray-600 hover:border-gray-300'
                      }`}
                    >
                      <input
                        type="radio"
                        name="petType"
                        checked={formData.petType === type.value}
                        onChange={() => handlePetTypeChange(type.value)}
                        className="hidden"
                      />
                      <MdPets />
                      {type.label}
                    </label>
                  ))}
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowModal(false);
                    resetForm();
                  }}
                  className="flex-1 px-4 py-3 border-2 border-gray-200 text-gray-700 rounded-xl hover:bg-gray-50 transition-colors"
                >
                  Hủy
                </button>
                <button type="submit" className="flex-1 btn-primary">
                  {editingService ? 'Cập nhật' : 'Thêm mới'}
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </div>
  );
};

export default AdminServicesPage;
