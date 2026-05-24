const API_BASE = "/api";
const STORAGE_KEY = "restaurant-management-user";

function readStoredUser() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed?.token ? parsed : null;
  } catch {
    return null;
  }
}

async function request(path, options = {}) {
  const currentUser = readStoredUser();
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(currentUser?.token ? { "X-Auth-Token": currentUser.token } : {}),
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(payload?.message || "请求失败");
  }

  if (payload && payload.code !== 200) {
    throw new Error(payload.message || "业务处理失败");
  }

  return payload?.data ?? null;
}

export const api = {
  health: () => request("/health"),
  dashboardOverview: () => request("/dashboard/overview"),
  analyticsSummary: () => request("/analytics/summary"),
  register: (data) =>
    request("/users/register", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  createStaff: (data) =>
    request("/users/staff", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  login: (data) =>
    request("/users/login", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  listTables: () => request("/tables"),
  createTable: (data) =>
    request("/tables", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  updateTable: (id, data) =>
    request(`/tables/${id}`, {
      method: "PUT",
      body: JSON.stringify(data)
    }),
  deleteTable: (id) =>
    request(`/tables/${id}`, {
      method: "DELETE"
    }),
  listMaterialCategories: () => request("/material-categories"),
  createMaterialCategory: (data) =>
    request("/material-categories", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  deleteMaterialCategory: (id) =>
    request(`/material-categories/${id}`, {
      method: "DELETE"
    }),
  listMaterials: () => request("/materials"),
  createMaterial: (data) =>
    request("/materials", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  updateMaterial: (id, data) =>
    request(`/materials/${id}`, {
      method: "PUT",
      body: JSON.stringify(data)
    }),
  deleteMaterial: (id) =>
    request(`/materials/${id}`, {
      method: "DELETE"
    }),
  refreshMaterialStatus: () =>
    request("/materials/refresh-status", {
      method: "POST"
    }),
  listDishCategories: () => request("/dish-categories"),
  createDishCategory: (data) =>
    request("/dish-categories", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  deleteDishCategory: (id) =>
    request(`/dish-categories/${id}`, {
      method: "DELETE"
    }),
  listDishes: () => request("/dishes"),
  createDish: (data) =>
    request("/dishes", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  updateDish: (id, data) =>
    request(`/dishes/${id}`, {
      method: "PUT",
      body: JSON.stringify(data)
    }),
  deleteDish: (id) =>
    request(`/dishes/${id}`, {
      method: "DELETE"
    }),
  getDishMaterials: (id) => request(`/dishes/${id}/materials`),
  bindDishMaterials: (id, data) =>
    request(`/dishes/${id}/materials`, {
      method: "POST",
      body: JSON.stringify(data)
    }),
  listOrders: () => request("/orders"),
  createOrder: (data) =>
    request("/orders", {
      method: "POST",
      body: JSON.stringify(data)
    }),
  payOrder: (id, data) =>
    request(`/orders/${id}/pay`, {
      method: "POST",
      body: JSON.stringify(data)
    }),
  prioritizeOrder: (id, data) =>
    request(`/orders/${id}/priority`, {
      method: "POST",
      body: JSON.stringify(data)
    }),
  completeOrder: (id) =>
    request(`/orders/${id}/complete`, {
      method: "POST"
    }),
  recordWaste: (id, data) =>
    request(`/orders/${id}/waste-records`, {
      method: "POST",
      body: JSON.stringify(data)
    }),
  kitchenQueue: () => request("/kitchen/queue"),
  startKitchenItem: (id) =>
    request(`/kitchen/items/${id}/start`, {
      method: "POST"
    }),
  readyKitchenItem: (id) =>
    request(`/kitchen/items/${id}/ready`, {
      method: "POST"
    }),
  serveKitchenItem: (id) =>
    request(`/kitchen/items/${id}/serve`, {
      method: "POST"
    }),
  urgeKitchenItem: (id) =>
    request(`/kitchen/items/${id}/urge`, {
      method: "POST"
    })
};
