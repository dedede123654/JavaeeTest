<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { api } from "./api";

const SECTION_TITLES = {
  customer: "顾客点餐",
  overview: "经营总览",
  auth: "员工账户",
  tables: "桌台管理",
  materials: "原料库存",
  dishes: "菜单中心",
  orders: "订单工作台",
  kitchen: "后厨看板",
  analytics: "经营分析"
};

const SECTION_ACCESS = {
  customer: [],
  overview: ["OWNER", "MANAGER", "WAITER", "CASHIER", "CHEF"],
  auth: [],
  tables: ["OWNER", "MANAGER", "WAITER", "CASHIER", "CHEF"],
  materials: ["OWNER", "MANAGER", "CHEF"],
  dishes: ["OWNER", "MANAGER"],
  orders: ["OWNER", "MANAGER", "WAITER", "CASHIER"],
  kitchen: ["OWNER", "MANAGER", "CHEF"],
  analytics: ["OWNER", "MANAGER"]
};

const ROLE_LABELS = {
  OWNER: "店长",
  MANAGER: "经理",
  WAITER: "服务员",
  CASHIER: "前台",
  CHEF: "后厨"
};

const TABLE_STATUS = {
  IDLE: { text: "空闲", className: "ok" },
  DINING: { text: "用餐中", className: "warn" }
};

const MATERIAL_STATUS = {
  0: { text: "库存正常", className: "ok" },
  1: { text: "低库存", className: "warn" },
  2: { text: "已沽清", className: "danger" }
};

const DISH_STATUS = {
  1: { text: "在售", className: "ok" },
  0: { text: "停售", className: "muted" }
};

const ORDER_STATUS = {
  PENDING_PAYMENT: { text: "待结账", className: "warn" },
  PAID: { text: "已支付", className: "ok" },
  WAITING_CLEAR: { text: "待清桌", className: "warn" },
  COMPLETED: { text: "已结单", className: "neutral" }
};

const ITEM_STATUS = {
  PENDING: { text: "待制作", className: "warn" },
  COOKING: { text: "制作中", className: "neutral" },
  READY: { text: "已出餐", className: "ok" },
  SERVED: { text: "已上桌", className: "ok" }
};

const WASTE_LEVELS = {
  CLEAN: { text: "光盘", className: "ok" },
  LEFTOVER_SOME: { text: "少量剩余", className: "warn" },
  LEFTOVER_MUCH: { text: "较多剩余", className: "danger" }
};

const ORDER_SOURCES = [
  { value: "CUSTOMER", label: "游客扫码" },
  { value: "WAITER", label: "服务员代下单" },
  { value: "CASHIER", label: "前台点单" }
];

const PAY_OPTIONS = ["移动支付", "现金", "刷卡"];
const ORDER_FILTERS = [
  { value: "ACTIVE", label: "今日待处理" },
  { value: "PENDING_PAYMENT", label: "待结账" },
  { value: "PAID", label: "已支付" },
  { value: "WAITING_CLEAR", label: "待清桌" },
  { value: "COMPLETED", label: "今日已完成" },
  { value: "ALL", label: "全部历史" }
];

const activeSection = ref("customer");
const loading = ref(false);
const health = ref("检查中");
const currentUser = ref(readStoredUser());
const message = reactive({
  type: "info",
  text: ""
});

const dashboard = ref(emptyDashboard());
const analytics = ref(emptyAnalytics());
const tables = ref([]);
const materialCategories = ref([]);
const materials = ref([]);
const dishCategories = ref([]);
const dishes = ref([]);
const orders = ref([]);
const kitchenQueue = ref([]);
const dishMaterials = reactive({});

const loginForm = reactive({
  username: "admin",
  password: "admin"
});

const registerForm = reactive({
  username: "",
  password: "",
  nickname: "",
  role: "WAITER",
  adminPassword: ""
});

const staffForm = reactive({
  username: "",
  password: "",
  nickname: "",
  role: "WAITER"
});

const tableForm = reactive(emptyTableForm());
const editingTableId = ref(null);

const materialCategoryForm = reactive({
  categoryName: ""
});

const materialForm = reactive(emptyMaterialForm());
const editingMaterialId = ref(null);

const dishCategoryForm = reactive({
  categoryName: "",
  sortOrder: "0"
});

const dishForm = reactive(emptyDishForm());
const editingDishId = ref(null);

const bindingDishId = ref(null);
const bindingDraft = ref([emptyBindingRow()]);

const orderForm = reactive(emptyOrderForm());
const orderStatusFilter = ref("ACTIVE");
const selectedOrderId = ref(null);

const paymentDrafts = reactive({});
const priorityDrafts = reactive({});
const wasteDrafts = reactive({});

const currentRole = computed(() => currentUser.value?.role || "");
const canViewAnalytics = computed(() => ["OWNER", "MANAGER"].includes(currentRole.value));
const canManageEmployees = computed(() => ["OWNER", "MANAGER"].includes(currentRole.value));
const canManageTables = computed(() => ["OWNER", "MANAGER"].includes(currentRole.value));
const canManageMenu = computed(() => ["OWNER", "MANAGER"].includes(currentRole.value));
const canManageMaterials = computed(() => ["OWNER", "MANAGER", "CHEF"].includes(currentRole.value));
const canCreateOrders = computed(() => ["OWNER", "MANAGER", "WAITER", "CASHIER"].includes(currentRole.value));
const canViewOrders = computed(() => ["OWNER", "MANAGER", "WAITER", "CASHIER"].includes(currentRole.value));
const canPayOrders = computed(() => ["OWNER", "MANAGER", "CASHIER"].includes(currentRole.value));
const canPrioritizeOrders = computed(() => ["OWNER", "MANAGER"].includes(currentRole.value));
const canRecordWaste = computed(() => ["OWNER", "MANAGER", "WAITER"].includes(currentRole.value));
const canUrgeOrders = computed(() => ["OWNER", "MANAGER", "WAITER"].includes(currentRole.value));
const canViewKitchenQueue = computed(() => ["OWNER", "MANAGER", "WAITER", "CHEF"].includes(currentRole.value));
const canStartCooking = computed(() => ["OWNER", "MANAGER", "CHEF"].includes(currentRole.value));
const canServeDish = computed(() => ["OWNER", "MANAGER", "WAITER", "CHEF"].includes(currentRole.value));
const visibleSections = computed(() => {
  if (!currentUser.value) {
    return [
      { key: "customer", label: SECTION_TITLES.customer },
      { key: "auth", label: SECTION_TITLES.auth }
    ];
  }
  return Object.entries(SECTION_TITLES)
    .filter(([key]) => {
      if (key === "customer") {
        return false;
      }
      if (key === "auth") {
        return canManageEmployees.value;
      }
      const allowedRoles = SECTION_ACCESS[key] || [];
      return allowedRoles.length === 0 || allowedRoles.includes(currentRole.value);
    })
    .map(([key, label]) => ({ key, label }));
});

const currentSectionTitle = computed(() => {
  const visible = visibleSections.value.find((item) => item.key === activeSection.value);
  return visible?.label || SECTION_TITLES.auth;
});

const overviewCards = computed(() => [
  { label: "空闲桌台", value: dashboard.value.idleTables || 0 },
  { label: "用餐中桌台", value: dashboard.value.diningTables || 0 },
  { label: "进行中订单", value: dashboard.value.activeOrders || 0 },
  { label: "后厨待处理", value: dashboard.value.pendingKitchenItems || 0 },
  { label: "低库存原料", value: dashboard.value.lowStockMaterials || 0 },
  { label: "提权订单", value: dashboard.value.priorityOrders || 0 },
  { label: "今日营收", value: formatMoney(dashboard.value.todayRevenue || 0) }
]);

const tableOptions = computed(() =>
  tables.value.map((item) => ({
    label: `${item.tableName} · ${item.areaName || "未分区"} · ${item.capacity}人`,
    value: item.id
  }))
);

const materialCategoryMap = computed(() => {
  const map = new Map();
  for (const item of materialCategories.value) {
    map.set(item.id, item.categoryName);
  }
  return map;
});

const dishCategoryMap = computed(() => {
  const map = new Map();
  for (const item of dishCategories.value) {
    map.set(item.id, item.categoryName);
  }
  return map;
});

const materialMap = computed(() => {
  const map = new Map();
  for (const item of materials.value) {
    map.set(item.id, item);
  }
  return map;
});

const activeDishes = computed(() => dishes.value.filter((item) => Number(item.status) === 1));
const dishMap = computed(() => {
  const map = new Map();
  for (const dish of dishes.value) {
    map.set(dish.id, dish);
  }
  return map;
});
const orderFormTotal = computed(() =>
  orderForm.items.reduce((sum, item) => sum + orderItemSubtotal(item), 0)
);
const filteredOrders = computed(() => {
  const today = currentLocalDateKey();
  return orders.value.filter((order) => {
    if (orderStatusFilter.value !== "ALL" && !String(order.createTime || "").startsWith(today)) {
      return false;
    }
    if (orderStatusFilter.value === "ACTIVE") {
      return order.orderStatus !== "COMPLETED";
    }
    if (orderStatusFilter.value === "ALL") {
      return true;
    }
    return order.orderStatus === orderStatusFilter.value;
  });
});
const selectedOrder = computed(() =>
  filteredOrders.value.find((order) => order.id === selectedOrderId.value) || null
);

function emptyDashboard() {
  return {
    idleTables: 0,
    diningTables: 0,
    activeOrders: 0,
    pendingKitchenItems: 0,
    lowStockMaterials: 0,
    priorityOrders: 0,
    todayRevenue: 0
  };
}

function emptyAnalytics() {
  return {
    totalRevenue: 0,
    completedOrders: 0,
    servedDishItems: 0,
    wasteRecordedItems: 0,
    wasteRate: 0,
    topSellingDishes: [],
    highWasteDishes: [],
    lowStockMaterials: []
  };
}

function emptyTableForm() {
  return {
    tableName: "",
    capacity: "4",
    areaName: "",
    status: "IDLE"
  };
}

function emptyMaterialForm() {
  return {
    name: "",
    categoryId: "",
    currentStock: "0",
    unit: "",
    warningStock: "0",
    status: 0
  };
}

function emptyDishForm() {
  return {
    categoryId: "",
    name: "",
    price: "0",
    estimatedTime: "10",
    imageUrl: "",
    description: "",
    status: 1
  };
}

function emptyBindingRow() {
  return {
    materialId: "",
    requiredQuantity: "1"
  };
}

function emptyOrderForm() {
  return {
    tableId: "",
    source: "CUSTOMER",
    remark: "",
    customerNote: "",
    items: [emptyOrderItemRow()]
  };
}

function emptyOrderItemRow() {
  return {
    isCustom: false,
    dishId: "",
    customDishName: "",
    customPrice: "0",
    quantity: "1",
    specialRequest: ""
  };
}

function defaultSectionForRole(role) {
  if (role === "CHEF") {
    return "kitchen";
  }
  if (role === "CASHIER") {
    return "orders";
  }
  if (role === "OWNER" || role === "MANAGER") {
    return "overview";
  }
  return "orders";
}

function readStoredUser() {
  const raw = localStorage.getItem("restaurant-management-user");
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

function isCustomerMode() {
  return !currentUser.value;
}

function writeStoredUser(user) {
  if (!user) {
    localStorage.removeItem("restaurant-management-user");
    return;
  }
  localStorage.setItem("restaurant-management-user", JSON.stringify(user));
}

function setMessage(text, type = "info") {
  message.text = text;
  message.type = type;
}

function clearMessage() {
  message.text = "";
  message.type = "info";
}

function currentLocalDateKey() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatBadge(map, key) {
  return map[key] || { text: "未知", className: "muted" };
}

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function formatDateTime(value) {
  if (!value) {
    return "未记录";
  }
  return value.replace("T", " ");
}

function formatShortTime(value) {
  if (!value) {
    return "--:--";
  }
  const normalized = value.replace("T", " ");
  return normalized.length >= 16 ? normalized.slice(11, 16) : normalized;
}

function orderItemCount(order) {
  return (order.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
}

function orderPendingCount(order) {
  return (order.items || []).filter((item) => item.itemStatus !== "SERVED").length;
}

function orderItemSubtotal(item) {
  const quantity = Number(item.quantity || 0);
  const price = item.isCustom
    ? Number(item.customPrice || 0)
    : Number(dishMap.value.get(Number(item.dishId))?.price || 0);
  return quantity * price;
}

function normalizeTableForm(form) {
  return {
    tableName: form.tableName.trim(),
    capacity: Number(form.capacity),
    areaName: form.areaName.trim() || null,
    status: form.status
  };
}

function normalizeMaterialForm(form) {
  return {
    name: form.name.trim(),
    categoryId: Number(form.categoryId),
    currentStock: Number(form.currentStock),
    unit: form.unit.trim(),
    warningStock: Number(form.warningStock),
    status: Number(form.status || 0)
  };
}

function normalizeDishForm(form) {
  return {
    categoryId: Number(form.categoryId),
    name: form.name.trim(),
    price: Number(form.price),
    estimatedTime: Number(form.estimatedTime),
    imageUrl: form.imageUrl.trim() || null,
    description: form.description.trim() || null,
    status: Number(form.status)
  };
}

function normalizeBindingDraft(rows) {
  return rows
    .filter((item) => item.materialId)
    .map((item) => ({
      materialId: Number(item.materialId),
      requiredQuantity: Number(item.requiredQuantity)
    }));
}

function normalizeOrderForm(form) {
  return {
    tableId: Number(form.tableId),
    source: isCustomerMode() ? "CUSTOMER" : form.source,
    remark: form.remark.trim() || null,
    customerNote: form.customerNote.trim() || null,
    items: form.items.map((item) => ({
      dishId: item.isCustom ? null : Number(item.dishId),
      customDishName: item.isCustom ? item.customDishName.trim() : null,
      customPrice: item.isCustom ? Number(item.customPrice) : null,
      quantity: Number(item.quantity),
      specialRequest: item.specialRequest.trim() || null
    }))
  };
}

function addDishToOrder(dish) {
  const existing = orderForm.items.find((item) => !item.isCustom && Number(item.dishId) === dish.id);
  if (existing) {
    existing.quantity = String(Number(existing.quantity || 0) + 1);
  } else if (orderForm.items.length === 1 && !orderForm.items[0].dishId && !orderForm.items[0].isCustom) {
    Object.assign(orderForm.items[0], {
      ...emptyOrderItemRow(),
      dishId: dish.id,
      quantity: "1"
    });
  } else {
    orderForm.items.push({
      ...emptyOrderItemRow(),
      dishId: dish.id,
      quantity: "1"
    });
  }
  setMessage(`${dish.name} 已加入点餐单`, "success");
}

function selectOrder(order) {
  selectedOrderId.value = order.id;
}

function closeOrderDetail() {
  selectedOrderId.value = null;
}

function ensureOrderDraft(order) {
  if (!paymentDrafts[order.id]) {
    paymentDrafts[order.id] = {
      paymentMethod: order.paymentMethod || "移动支付",
      discountAmount: order.discountAmount ?? 0
    };
  }
  if (!priorityDrafts[order.id]) {
    priorityDrafts[order.id] = {
      priorityLevel: order.priorityLevel > 0 ? order.priorityLevel : 1,
      priorityReason: order.priorityReason || ""
    };
  }
  if (!wasteDrafts[order.id]) {
    wasteDrafts[order.id] = {};
  }
  for (const item of order.items || []) {
    if (!wasteDrafts[order.id][item.id]) {
      wasteDrafts[order.id][item.id] = {
        wasteLevel: item.wasteLevel || "CLEAN",
        note: item.wasteNote || ""
      };
      continue;
    }
    wasteDrafts[order.id][item.id].wasteLevel = item.wasteLevel || wasteDrafts[order.id][item.id].wasteLevel || "CLEAN";
    wasteDrafts[order.id][item.id].note = item.wasteNote || wasteDrafts[order.id][item.id].note || "";
  }
}

function resetViewData() {
  dashboard.value = emptyDashboard();
  analytics.value = emptyAnalytics();
  tables.value = [];
  materialCategories.value = [];
  materials.value = [];
  dishCategories.value = [];
  dishes.value = [];
  orders.value = [];
  kitchenQueue.value = [];
}

async function withLoading(action, successText) {
  loading.value = true;
  clearMessage();
  try {
    await action();
    if (successText) {
      setMessage(successText, "success");
    }
  } catch (error) {
    setMessage(error.message || "操作失败", "error");
  } finally {
    loading.value = false;
  }
}

async function fetchHealth() {
  try {
    const data = await api.health();
    health.value = data?.status === "ok" ? "服务正常" : "服务异常";
  } catch (error) {
    health.value = `接口不可用：${error.message}`;
  }
}

async function refreshAll() {
  if (!currentUser.value) {
    await refreshPublicMenu();
    return;
  }

  const analyticsPromise = canViewAnalytics.value ? api.analyticsSummary() : Promise.resolve(emptyAnalytics());
  const ordersPromise = canViewOrders.value ? api.listOrders() : Promise.resolve([]);
  const kitchenPromise = canViewKitchenQueue.value ? api.kitchenQueue() : Promise.resolve([]);
  const [
    dashboardData,
    analyticsData,
    tableData,
    materialCategoryData,
    materialData,
    dishCategoryData,
    dishData,
    orderData,
    kitchenData
  ] = await Promise.all([
    api.dashboardOverview(),
    analyticsPromise,
    api.listTables(),
    api.listMaterialCategories(),
    api.listMaterials(),
    api.listDishCategories(),
    api.listDishes(),
    ordersPromise,
    kitchenPromise
  ]);

  dashboard.value = dashboardData || emptyDashboard();
  analytics.value = analyticsData || emptyAnalytics();
  tables.value = tableData || [];
  materialCategories.value = materialCategoryData || [];
  materials.value = materialData || [];
  dishCategories.value = dishCategoryData || [];
  dishes.value = dishData || [];
  orders.value = orderData || [];
  kitchenQueue.value = kitchenData || [];

  for (const order of orders.value) {
    ensureOrderDraft(order);
  }
}

async function refreshPublicMenu() {
  const [tableData, dishCategoryData, dishData] = await Promise.all([
    api.listTables(),
    api.listDishCategories(),
    api.listDishes()
  ]);
  resetViewData();
  tables.value = tableData || [];
  dishCategories.value = dishCategoryData || [];
  dishes.value = dishData || [];
}

async function loadDishMaterials(dishId) {
  if (!dishId) {
    return;
  }
  dishMaterials[dishId] = (await api.getDishMaterials(dishId)) || [];
}

async function ensureAllDishMaterialsLoaded() {
  for (const dish of dishes.value) {
    await loadDishMaterials(dish.id);
  }
}

async function initializePage() {
  await fetchHealth();
  if (!currentUser.value) {
    await withLoading(async () => {
      await refreshPublicMenu();
    });
    activeSection.value = "customer";
    return;
  }
  await withLoading(async () => {
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
  });
}

async function handleLogin() {
  await withLoading(async () => {
    const user = await api.login(loginForm);
    currentUser.value = user;
    writeStoredUser(user);
    activeSection.value = defaultSectionForRole(user.role);
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
  }, "登录成功");
}

async function handleRegister() {
  await withLoading(async () => {
    const user = await api.register(registerForm);
    currentUser.value = user;
    writeStoredUser(user);
    activeSection.value = defaultSectionForRole(user.role);
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
    Object.assign(registerForm, {
      username: "",
      password: "",
      nickname: "",
      role: "WAITER",
      adminPassword: ""
    });
  }, "注册成功");
}

async function handleCreateStaff() {
  await withLoading(async () => {
    await api.createStaff(staffForm);
    Object.assign(staffForm, {
      username: "",
      password: "",
      nickname: "",
      role: "WAITER"
    });
  }, "员工账号已创建");
}

function logout() {
  currentUser.value = null;
  writeStoredUser(null);
  resetViewData();
  activeSection.value = "customer";
  setMessage("已清除本地登录状态", "success");
  refreshPublicMenu();
}

function editTable(item) {
  editingTableId.value = item.id;
  Object.assign(tableForm, {
    tableName: item.tableName || "",
    capacity: item.capacity ?? "4",
    areaName: item.areaName || "",
    status: item.status || "IDLE"
  });
  activeSection.value = "tables";
}

function resetTableForm() {
  editingTableId.value = null;
  Object.assign(tableForm, emptyTableForm());
}

async function submitTable() {
  const payload = normalizeTableForm(tableForm);
  await withLoading(async () => {
    if (editingTableId.value) {
      await api.updateTable(editingTableId.value, payload);
    } else {
      await api.createTable(payload);
    }
    resetTableForm();
    await refreshAll();
  }, editingTableId.value ? "桌台已更新" : "桌台已创建");
}

async function removeTable(id) {
  await withLoading(async () => {
    await api.deleteTable(id);
    await refreshAll();
  }, "桌台已删除");
}

async function submitMaterialCategory() {
  await withLoading(async () => {
    await api.createMaterialCategory({
      categoryName: materialCategoryForm.categoryName.trim()
    });
    materialCategoryForm.categoryName = "";
    await refreshAll();
  }, "原料分类已新增");
}

async function removeMaterialCategory(id) {
  await withLoading(async () => {
    await api.deleteMaterialCategory(id);
    await refreshAll();
  }, "原料分类已删除");
}

function editMaterial(item) {
  editingMaterialId.value = item.id;
  Object.assign(materialForm, {
    name: item.name || "",
    categoryId: item.categoryId ?? "",
    currentStock: item.currentStock ?? "0",
    unit: item.unit || "",
    warningStock: item.warningStock ?? "0",
    status: item.status ?? 0
  });
  activeSection.value = "materials";
}

function resetMaterialForm() {
  editingMaterialId.value = null;
  Object.assign(materialForm, emptyMaterialForm());
}

async function submitMaterial() {
  const payload = normalizeMaterialForm(materialForm);
  await withLoading(async () => {
    if (editingMaterialId.value) {
      await api.updateMaterial(editingMaterialId.value, payload);
    } else {
      await api.createMaterial(payload);
    }
    resetMaterialForm();
    await refreshAll();
  }, editingMaterialId.value ? "原料已更新" : "原料已新增");
}

async function removeMaterial(id) {
  await withLoading(async () => {
    await api.deleteMaterial(id);
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
  }, "原料已删除");
}

async function refreshMaterialStatus() {
  await withLoading(async () => {
    materials.value = await api.refreshMaterialStatus();
    dashboard.value = await api.dashboardOverview();
  }, "库存状态已刷新");
}

async function submitDishCategory() {
  await withLoading(async () => {
    await api.createDishCategory({
      categoryName: dishCategoryForm.categoryName.trim(),
      sortOrder: Number(dishCategoryForm.sortOrder)
    });
    Object.assign(dishCategoryForm, {
      categoryName: "",
      sortOrder: "0"
    });
    await refreshAll();
  }, "菜品分类已新增");
}

async function removeDishCategory(id) {
  await withLoading(async () => {
    await api.deleteDishCategory(id);
    await refreshAll();
  }, "菜品分类已删除");
}

function editDish(item) {
  editingDishId.value = item.id;
  Object.assign(dishForm, {
    categoryId: item.categoryId ?? "",
    name: item.name || "",
    price: item.price ?? "0",
    estimatedTime: item.estimatedTime ?? "10",
    imageUrl: item.imageUrl || "",
    description: item.description || "",
    status: item.status ?? 1
  });
  activeSection.value = "dishes";
}

function resetDishForm() {
  editingDishId.value = null;
  Object.assign(dishForm, emptyDishForm());
}

async function submitDish() {
  const payload = normalizeDishForm(dishForm);
  await withLoading(async () => {
    if (editingDishId.value) {
      await api.updateDish(editingDishId.value, payload);
    } else {
      await api.createDish(payload);
    }
    resetDishForm();
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
  }, editingDishId.value ? "菜品已更新" : "菜品已新增");
}

async function removeDish(id) {
  await withLoading(async () => {
    await api.deleteDish(id);
    delete dishMaterials[id];
    await refreshAll();
  }, "菜品已删除");
}

function openBinding(dish) {
  const existing = dishMaterials[dish.id] || [];
  bindingDishId.value = dish.id;
  bindingDraft.value =
    existing.length > 0
      ? existing.map((item) => ({
          materialId: item.materialId,
          requiredQuantity: item.requiredQuantity
        }))
      : [emptyBindingRow()];
}

function addBindingRow() {
  bindingDraft.value.push(emptyBindingRow());
}

function removeBindingRow(index) {
  if (bindingDraft.value.length === 1) {
    bindingDraft.value[0] = emptyBindingRow();
    return;
  }
  bindingDraft.value.splice(index, 1);
}

async function submitBinding() {
  const dishId = bindingDishId.value;
  const payload = normalizeBindingDraft(bindingDraft.value);
  await withLoading(async () => {
    await api.bindDishMaterials(dishId, payload);
    await loadDishMaterials(dishId);
    await refreshAll();
  }, "菜品配料已更新");
}

function addOrderItemRow() {
  orderForm.items.push(emptyOrderItemRow());
}

function removeOrderItemRow(index) {
  if (orderForm.items.length === 1) {
    orderForm.items[0] = emptyOrderItemRow();
    return;
  }
  orderForm.items.splice(index, 1);
}

function resetOrderForm() {
  Object.assign(orderForm, emptyOrderForm());
  orderForm.source = currentRole.value === "CASHIER" ? "CASHIER" : currentUser.value ? "WAITER" : "CUSTOMER";
}

async function submitOrder() {
  const payload = normalizeOrderForm(orderForm);
  await withLoading(async () => {
    await api.createOrder(payload);
    resetOrderForm();
    await refreshAll();
    await ensureAllDishMaterialsLoaded();
  }, "订单已创建");
}

async function payOrder(order) {
  const draft = paymentDrafts[order.id];
  await withLoading(async () => {
    await api.payOrder(order.id, {
      paymentMethod: draft.paymentMethod,
      discountAmount: Number(draft.discountAmount || 0)
    });
    await refreshAll();
  }, "订单已收款");
}

async function prioritizeOrder(order) {
  const draft = priorityDrafts[order.id];
  await withLoading(async () => {
    await api.prioritizeOrder(order.id, {
      priorityLevel: Number(draft.priorityLevel),
      priorityReason: draft.priorityReason.trim() || null
    });
    await refreshAll();
  }, "订单已提权");
}

async function completeOrder(orderId) {
  await withLoading(async () => {
    await api.completeOrder(orderId);
    await refreshAll();
  }, "订单已结单");
}

async function submitWasteRecords(order) {
  const servedItems = (order.items || []).filter((item) => item.itemStatus === "SERVED");
  if (!servedItems.length) {
    setMessage("当前订单没有可记录光盘情况的菜品", "error");
    return;
  }
  const payload = servedItems.map((item) => ({
    orderItemId: item.id,
    wasteLevel: wasteDrafts[order.id]?.[item.id]?.wasteLevel || "CLEAN",
    note: wasteDrafts[order.id]?.[item.id]?.note?.trim() || null
  }));
  await withLoading(async () => {
    await api.recordWaste(order.id, payload);
    await refreshAll();
  }, "光盘记录已提交");
}

async function urgeItem(itemId) {
  await withLoading(async () => {
    await api.urgeKitchenItem(itemId);
    await refreshAll();
  }, "已发送催单提醒");
}

async function startKitchenItem(itemId) {
  await withLoading(async () => {
    await api.startKitchenItem(itemId);
    await refreshAll();
  }, "菜品状态已更新为制作中");
}

async function readyKitchenItem(itemId) {
  await withLoading(async () => {
    await api.readyKitchenItem(itemId);
    await refreshAll();
  }, "菜品状态已更新为已出餐");
}

async function serveKitchenItem(itemId) {
  await withLoading(async () => {
    await api.serveKitchenItem(itemId);
    await refreshAll();
  }, "菜品状态已更新为已上桌");
}

function bindingMaterialName(materialId) {
  return materialMap.value.get(materialId)?.name || `原料 #${materialId}`;
}

function bindingMaterialUnit(materialId) {
  return materialMap.value.get(materialId)?.unit || "";
}

watch(
  [currentUser, visibleSections],
  ([user, sections]) => {
    const allowedKeys = sections.map((item) => item.key);
    if (!allowedKeys.includes(activeSection.value)) {
      activeSection.value = user ? defaultSectionForRole(user.role) : "auth";
      if (!allowedKeys.includes(activeSection.value)) {
        activeSection.value = allowedKeys[0] || "auth";
      }
    }
  },
  { immediate: true }
);

onMounted(async () => {
  await initializePage();
});
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div>
        <p class="eyebrow">Restaurant Console</p>
        <h1>餐厅管理系统</h1>
        <p class="subtle">
          用现有 Spring Boot + Vue 框架重构为餐厅业务工作台，覆盖桌台、库存、点单、后厨与结账主流程。
        </p>
      </div>

      <div class="status-card">
        <span class="dot"></span>
        <div>
          <strong>服务状态</strong>
          <p>{{ health }}</p>
        </div>
      </div>

      <nav class="nav-list">
        <button
          v-for="item in visibleSections"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeSection === item.key }"
          @click="activeSection = item.key"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="user-card">
        <template v-if="currentUser">
          <p class="user-label">当前员工</p>
          <strong>{{ currentUser.nickname }}</strong>
          <span>{{ currentUser.username }} · {{ ROLE_LABELS[currentUser.role] || currentUser.role }}</span>
          <button class="ghost-button" @click="logout">退出本地状态</button>
        </template>
        <template v-else>
          <p class="user-label">顾客模式</p>
          <span>可直接点餐；员工入口在左侧“员工登录”。</span>
        </template>
      </div>
    </aside>

    <main class="content">
      <header class="hero">
        <div>
          <p class="eyebrow">Workspace</p>
          <h2>{{ currentSectionTitle }}</h2>
          <p class="subtle">所有接口统一走 `/api`，前端构建后由 Spring Boot 直接托管。</p>
        </div>
        <button class="primary-button" :disabled="loading" @click="initializePage">
          {{ loading ? "同步中..." : "刷新全部数据" }}
        </button>
      </header>

      <div v-if="message.text" class="message" :class="message.type">
        {{ message.text }}
      </div>

      <section v-show="activeSection === 'customer'" class="panel customer-panel">
        <div class="customer-hero card">
          <div>
            <p class="eyebrow">Customer Order</p>
            <h3>欢迎点餐</h3>
            <p class="subtle">先选择桌台，再从菜单加入菜品；员工可切换到登录入口处理后厨、收款和管理。</p>
          </div>
          <button class="ghost-button" type="button" @click="activeSection = 'auth'">员工登录</button>
        </div>

        <div class="customer-layout">
          <article class="card">
            <div class="section-head">
              <h3>菜单</h3>
              <span>{{ activeDishes.length }} 道在售</span>
            </div>
            <div class="category-strip">
              <span v-for="category in dishCategories" :key="category.id" class="tag">
                {{ category.categoryName }}
              </span>
            </div>
            <div class="customer-menu-grid">
              <article v-for="dish in activeDishes" :key="dish.id" class="menu-card">
                <div>
                  <strong>{{ dish.name }}</strong>
                  <p>{{ dishCategoryMap.get(dish.categoryId) || "未分类" }} · {{ dish.estimatedTime }} 分钟</p>
                </div>
                <div class="menu-card-bottom">
                  <span>{{ formatMoney(dish.price) }}</span>
                  <button class="primary-button secondary" type="button" :disabled="loading" @click="addDishToOrder(dish)">
                    加入
                  </button>
                </div>
              </article>
            </div>
          </article>

          <article class="card sticky-cart">
            <div class="section-head">
              <h3>点餐单</h3>
              <button type="button" class="ghost-button" @click="resetOrderForm">清空</button>
            </div>
            <form class="form-grid" @submit.prevent="submitOrder">
              <label>
                当前桌台
                <select v-model="orderForm.tableId">
                  <option disabled value="">请选择桌台</option>
                  <option v-for="option in tableOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
              <div class="cart-list">
                <div v-for="(row, index) in orderForm.items" :key="index" class="cart-row">
                  <select v-model="row.dishId">
                    <option disabled value="">选择菜品</option>
                    <option v-for="dish in activeDishes" :key="dish.id" :value="dish.id">
                      {{ dish.name }}（{{ formatMoney(dish.price) }}）
                    </option>
                  </select>
                  <input v-model="row.quantity" type="number" min="1" step="1" placeholder="数量" />
                  <input v-model="row.specialRequest" type="text" placeholder="口味备注" />
                  <button type="button" class="ghost-button" @click="removeOrderItemRow(index)">移除</button>
                </div>
              </div>
              <label>
                顾客备注
                <input v-model="orderForm.customerNote" type="text" placeholder="例如：少辣、忌口、餐具需求" />
              </label>
              <div class="order-summary">
                <span>合计</span>
                <strong>{{ formatMoney(orderFormTotal) }}</strong>
              </div>
              <div class="inline-actions">
                <button type="button" class="ghost-button" @click="addOrderItemRow">手动加菜</button>
                <button class="primary-button" :disabled="loading">提交点餐</button>
              </div>
            </form>
          </article>
        </div>
      </section>

      <section v-show="activeSection === 'overview'" class="panel">
        <div class="card-grid">
          <article v-for="card in overviewCards" :key="card.label" class="metric-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </div>

        <div class="two-column">
          <article class="card">
            <div class="section-head">
              <h3>当前后厨队列</h3>
              <span>{{ kitchenQueue.length }} 项</span>
            </div>
            <div v-if="kitchenQueue.length" class="stack-list">
              <div v-for="item in kitchenQueue.slice(0, 6)" :key="item.itemId" class="list-row">
                <div>
                  <strong>{{ item.dishName }} × {{ item.quantity }}</strong>
                  <p>{{ item.tableName }} · 等待 {{ item.waitMinutes }} 分钟</p>
                </div>
                <span class="badge" :class="formatBadge(ITEM_STATUS, item.itemStatus).className">
                  {{ formatBadge(ITEM_STATUS, item.itemStatus).text }}
                </span>
              </div>
            </div>
            <p v-else class="empty">当前后厨队列为空。</p>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>低库存原料</h3>
              <span>{{ materials.filter((item) => Number(item.status) > 0).length }} 项</span>
            </div>
            <div v-if="materials.length" class="stack-list">
              <div
                v-for="item in materials.filter((material) => Number(material.status) > 0).slice(0, 6)"
                :key="item.id"
                class="list-row"
              >
                <div>
                  <strong>{{ item.name }}</strong>
                  <p>{{ item.currentStock }} {{ item.unit }} / 预警 {{ item.warningStock }} {{ item.unit }}</p>
                </div>
                <span class="badge" :class="formatBadge(MATERIAL_STATUS, item.status).className">
                  {{ formatBadge(MATERIAL_STATUS, item.status).text }}
                </span>
              </div>
            </div>
            <p v-else class="empty">当前没有原料数据。</p>
          </article>
        </div>
      </section>

      <section v-show="activeSection === 'auth'" class="panel two-column">
        <template v-if="!currentUser">
          <article class="card">
            <div class="section-head">
              <h3>登录</h3>
              <span>默认管理员可直接体验</span>
            </div>
            <form class="form-grid" @submit.prevent="handleLogin">
              <label>
                用户名
                <input v-model="loginForm.username" type="text" />
              </label>
              <label>
                密码
                <input v-model="loginForm.password" type="password" />
              </label>
              <button class="primary-button" :disabled="loading">登录</button>
            </form>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>注册员工</h3>
              <span>可注册任意角色，但需输入管理员密码</span>
            </div>
            <form class="form-grid" @submit.prevent="handleRegister">
              <label>
                用户名
                <input v-model="registerForm.username" type="text" />
              </label>
              <label>
                密码
                <input v-model="registerForm.password" type="password" />
              </label>
              <label>
                昵称
                <input v-model="registerForm.nickname" type="text" />
              </label>
              <label>
                角色
                <select v-model="registerForm.role">
                  <option value="WAITER">服务员</option>
                  <option value="CASHIER">前台</option>
                  <option value="CHEF">后厨</option>
                  <option value="MANAGER">经理</option>
                  <option value="OWNER">店长</option>
                </select>
              </label>
              <label>
                管理员密码
                <input v-model="registerForm.adminPassword" type="password" placeholder="请输入密码" />
              </label>
              <button class="primary-button" :disabled="loading">注册</button>
            </form>
          </article>
        </template>

        <template v-else-if="canManageEmployees">
          <article class="card">
            <div class="section-head">
              <h3>创建员工账号</h3>
              <span>管理员可按权限创建不同岗位</span>
            </div>
            <form class="form-grid" @submit.prevent="handleCreateStaff">
              <label>
                用户名
                <input v-model="staffForm.username" type="text" />
              </label>
              <label>
                密码
                <input v-model="staffForm.password" type="password" />
              </label>
              <label>
                昵称
                <input v-model="staffForm.nickname" type="text" />
              </label>
              <label>
                角色
                <select v-model="staffForm.role">
                  <option value="WAITER">服务员</option>
                  <option value="CASHIER">前台</option>
                  <option value="CHEF">后厨</option>
                  <option v-if="currentRole === 'OWNER'" value="MANAGER">经理</option>
                  <option v-if="currentRole === 'OWNER'" value="OWNER">店长</option>
                </select>
              </label>
              <button class="primary-button" :disabled="loading">创建账号</button>
            </form>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>创建规则</h3>
              <span>避免公开注册越权</span>
            </div>
            <div class="stack-list">
              <div class="list-row">
                <div>
                  <strong>公开注册</strong>
                  <p>未登录状态下只能创建服务员账号。</p>
                </div>
              </div>
              <div class="list-row">
                <div>
                  <strong>店长</strong>
                  <p>可创建店长、经理、服务员、前台、后厨。</p>
                </div>
              </div>
              <div class="list-row">
                <div>
                  <strong>经理</strong>
                  <p>可创建服务员、前台、后厨，不能创建经理和店长。</p>
                </div>
              </div>
            </div>
          </article>
        </template>
      </section>

      <section v-show="activeSection === 'tables'" class="panel two-column">
        <article class="card">
          <div class="section-head">
            <h3>{{ editingTableId ? "编辑桌台" : "新增桌台" }}</h3>
            <button type="button" class="ghost-button" @click="resetTableForm">重置表单</button>
          </div>
          <form class="form-grid two-up" @submit.prevent="submitTable">
            <label>
              桌号
              <input v-model="tableForm.tableName" type="text" placeholder="例如 A01" />
            </label>
            <label>
              容纳人数
              <input v-model="tableForm.capacity" type="number" min="1" />
            </label>
            <label>
              区域
              <input v-model="tableForm.areaName" type="text" placeholder="大厅 / 窗边 / 包间" />
            </label>
            <label>
              状态
              <select v-model="tableForm.status">
                <option value="IDLE">空闲</option>
                <option value="DINING">用餐中</option>
              </select>
            </label>
            <button class="primary-button" :disabled="loading || !canManageTables">
              {{ editingTableId ? "更新桌台" : "创建桌台" }}
            </button>
          </form>
        </article>

        <article class="card">
          <div class="section-head">
            <h3>桌台列表</h3>
            <span>{{ tables.length }} 张</span>
          </div>
          <div class="stack-list">
            <div v-for="item in tables" :key="item.id" class="list-row">
              <div>
                <strong>{{ item.tableName }}</strong>
                <p>{{ item.areaName || "未分区" }} · {{ item.capacity }} 人</p>
              </div>
              <div class="actions">
                <span class="badge" :class="formatBadge(TABLE_STATUS, item.status).className">
                  {{ formatBadge(TABLE_STATUS, item.status).text }}
                </span>
                <button class="ghost-button" :disabled="!canManageTables" @click="editTable(item)">编辑</button>
                <button class="danger-button" :disabled="!canManageTables" @click="removeTable(item.id)">删除</button>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section v-show="activeSection === 'materials'" class="panel">
        <div class="two-column">
          <article class="card">
            <div class="section-head">
              <h3>原料分类</h3>
              <span>{{ materialCategories.length }} 类</span>
            </div>
            <form class="form-grid" @submit.prevent="submitMaterialCategory">
              <label>
                分类名称
                <input v-model="materialCategoryForm.categoryName" type="text" placeholder="例如：海鲜" />
              </label>
              <button class="primary-button" :disabled="loading || !canManageMaterials">新增分类</button>
            </form>
            <div class="stack-list compact">
              <div v-for="item in materialCategories" :key="item.id" class="list-row">
                <div>
                  <strong>{{ item.categoryName }}</strong>
                </div>
                <button class="danger-button" :disabled="!canManageMaterials" @click="removeMaterialCategory(item.id)">删除</button>
              </div>
            </div>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>{{ editingMaterialId ? "编辑原料" : "新增原料" }}</h3>
              <div class="actions">
                <button type="button" class="ghost-button" @click="resetMaterialForm">重置表单</button>
                <button type="button" class="primary-button secondary" :disabled="loading || !canManageMaterials" @click="refreshMaterialStatus">
                  刷新库存状态
                </button>
              </div>
            </div>
            <form class="form-grid two-up" @submit.prevent="submitMaterial">
              <label>
                原料名称
                <input v-model="materialForm.name" type="text" />
              </label>
              <label>
                分类
                <select v-model="materialForm.categoryId">
                  <option disabled value="">请选择分类</option>
                  <option v-for="item in materialCategories" :key="item.id" :value="item.id">
                    {{ item.categoryName }}
                  </option>
                </select>
              </label>
              <label>
                当前库存
                <input v-model="materialForm.currentStock" type="number" min="0" step="0.01" />
              </label>
              <label>
                单位
                <input v-model="materialForm.unit" type="text" placeholder="份 / 碗 / 杯" />
              </label>
              <label>
                预警库存
                <input v-model="materialForm.warningStock" type="number" min="0" step="0.01" />
              </label>
              <button class="primary-button" :disabled="loading || !canManageMaterials">
                {{ editingMaterialId ? "更新原料" : "创建原料" }}
              </button>
            </form>
          </article>
        </div>

        <article class="card table-card">
          <div class="section-head">
            <h3>原料库存列表</h3>
            <span>{{ materials.length }} 条</span>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>名称</th>
                  <th>分类</th>
                  <th>库存</th>
                  <th>预警值</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in materials" :key="item.id">
                  <td>{{ item.name }}</td>
                  <td>{{ materialCategoryMap.get(item.categoryId) || `分类 #${item.categoryId}` }}</td>
                  <td>{{ item.currentStock }} {{ item.unit }}</td>
                  <td>{{ item.warningStock }} {{ item.unit }}</td>
                  <td>
                    <span class="badge" :class="formatBadge(MATERIAL_STATUS, item.status).className">
                      {{ formatBadge(MATERIAL_STATUS, item.status).text }}
                    </span>
                  </td>
                  <td class="actions">
                    <button class="ghost-button" :disabled="!canManageMaterials" @click="editMaterial(item)">编辑</button>
                    <button class="danger-button" :disabled="!canManageMaterials" @click="removeMaterial(item.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-show="activeSection === 'dishes'" class="panel">
        <div class="two-column">
          <article class="card">
            <div class="section-head">
              <h3>菜品分类</h3>
              <span>{{ dishCategories.length }} 类</span>
            </div>
            <form class="form-grid two-up" @submit.prevent="submitDishCategory">
              <label>
                分类名称
                <input v-model="dishCategoryForm.categoryName" type="text" />
              </label>
              <label>
                排序
                <input v-model="dishCategoryForm.sortOrder" type="number" min="0" />
              </label>
              <button class="primary-button" :disabled="loading || !canManageMenu">新增分类</button>
            </form>
            <div class="stack-list compact">
              <div v-for="item in dishCategories" :key="item.id" class="list-row">
                <div>
                  <strong>{{ item.categoryName }}</strong>
                  <p>排序 {{ item.sortOrder }}</p>
                </div>
                <button class="danger-button" :disabled="!canManageMenu" @click="removeDishCategory(item.id)">删除</button>
              </div>
            </div>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>{{ editingDishId ? "编辑菜品" : "新增菜品" }}</h3>
              <button type="button" class="ghost-button" @click="resetDishForm">重置表单</button>
            </div>
            <form class="form-grid two-up" @submit.prevent="submitDish">
              <label>
                菜品名称
                <input v-model="dishForm.name" type="text" />
              </label>
              <label>
                分类
                <select v-model="dishForm.categoryId">
                  <option disabled value="">请选择分类</option>
                  <option v-for="item in dishCategories" :key="item.id" :value="item.id">
                    {{ item.categoryName }}
                  </option>
                </select>
              </label>
              <label>
                售价
                <input v-model="dishForm.price" type="number" min="0" step="0.01" />
              </label>
              <label>
                预计时长（分钟）
                <input v-model="dishForm.estimatedTime" type="number" min="1" />
              </label>
              <label>
                图片地址
                <input v-model="dishForm.imageUrl" type="text" placeholder="可留空" />
              </label>
              <label>
                状态
                <select v-model="dishForm.status">
                  <option :value="1">在售</option>
                  <option :value="0">停售</option>
                </select>
              </label>
              <label class="full-span">
                描述
                <textarea v-model="dishForm.description" rows="4"></textarea>
              </label>
              <button class="primary-button" :disabled="loading || !canManageMenu">
                {{ editingDishId ? "更新菜品" : "创建菜品" }}
              </button>
            </form>
          </article>
        </div>

        <article class="card" v-if="bindingDishId">
          <div class="section-head">
            <h3>绑定菜品配料</h3>
            <span>菜品 ID {{ bindingDishId }}</span>
          </div>
          <div class="binding-list">
            <div v-for="(row, index) in bindingDraft" :key="index" class="binding-row">
              <select v-model="row.materialId">
                <option disabled value="">选择原料</option>
                <option v-for="item in materials" :key="item.id" :value="item.id">
                  {{ item.name }}（{{ item.currentStock }} {{ item.unit }}）
                </option>
              </select>
              <input v-model="row.requiredQuantity" type="number" min="0" step="0.01" placeholder="用量" />
                <button type="button" class="ghost-button" @click="removeBindingRow(index)">移除</button>
            </div>
          </div>
          <div class="inline-actions">
            <button type="button" class="ghost-button" @click="addBindingRow">新增一行</button>
            <button type="button" class="primary-button" :disabled="loading || !canManageMenu" @click="submitBinding">保存配料</button>
          </div>
        </article>

        <div class="recipe-grid">
          <article v-for="dish in dishes" :key="dish.id" class="recipe-card">
            <div class="recipe-top">
              <div>
                <p class="eyebrow">#{{ dish.id }}</p>
                <h3>{{ dish.name }}</h3>
              </div>
              <span class="badge" :class="formatBadge(DISH_STATUS, dish.status).className">
                {{ formatBadge(DISH_STATUS, dish.status).text }}
              </span>
            </div>
            <p class="recipe-meta">
              {{ dishCategoryMap.get(dish.categoryId) || `分类 #${dish.categoryId}` }} · {{ formatMoney(dish.price) }} ·
              {{ dish.estimatedTime }} 分钟
            </p>
            <p class="recipe-steps">{{ dish.description || "暂无菜品说明" }}</p>

            <div class="tag-list">
              <span
                v-for="binding in dishMaterials[dish.id] || []"
                :key="binding.id || `${binding.materialId}-${binding.requiredQuantity}`"
                class="tag"
              >
                {{ bindingMaterialName(binding.materialId) }} {{ binding.requiredQuantity }} {{ bindingMaterialUnit(binding.materialId) }}
              </span>
              <span v-if="!(dishMaterials[dish.id] || []).length" class="tag empty-tag">未配置配料</span>
            </div>

            <div class="actions recipe-actions">
              <button class="ghost-button" :disabled="!canManageMenu" @click="editDish(dish)">编辑</button>
              <button class="ghost-button" :disabled="!canManageMenu" @click="openBinding(dish)">绑定配料</button>
              <button class="danger-button" :disabled="!canManageMenu" @click="removeDish(dish.id)">删除</button>
            </div>
          </article>
        </div>
      </section>

      <section v-show="activeSection === 'orders'" class="panel">
        <div class="two-column">
          <article class="card">
            <div class="section-head">
              <h3>新建订单</h3>
              <button type="button" class="ghost-button" @click="resetOrderForm">重置表单</button>
            </div>
            <form class="form-grid" @submit.prevent="submitOrder">
              <div class="form-grid two-up">
                <label>
                  桌台
                  <select v-model="orderForm.tableId">
                    <option disabled value="">请选择桌台</option>
                    <option v-for="option in tableOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </label>
                <label>
                  下单来源
                  <select v-model="orderForm.source">
                    <option v-for="item in ORDER_SOURCES" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </option>
                  </select>
                </label>
              </div>
              <label>
                订单备注
                <input v-model="orderForm.remark" type="text" placeholder="例如：生日聚餐、需要快上" />
              </label>
              <label>
                顾客备注
                <input v-model="orderForm.customerNote" type="text" placeholder="例如：少辣、忌葱" />
              </label>
              <div class="binding-list">
                <div v-for="(row, index) in orderForm.items" :key="index" class="order-item-row">
                  <label class="inline-check">
                    <input v-model="row.isCustom" type="checkbox" />
                    定制菜
                  </label>
                  <template v-if="row.isCustom">
                    <input v-model="row.customDishName" type="text" placeholder="定制菜名" />
                    <input v-model="row.customPrice" type="number" min="0" step="0.01" placeholder="单价" />
                  </template>
                  <template v-else>
                    <select v-model="row.dishId">
                      <option disabled value="">选择菜品</option>
                      <option v-for="dish in activeDishes" :key="dish.id" :value="dish.id">
                        {{ dish.name }}（{{ formatMoney(dish.price) }}）
                      </option>
                    </select>
                  </template>
                  <input v-model="row.quantity" type="number" min="1" step="1" placeholder="数量" />
                  <input v-model="row.specialRequest" type="text" placeholder="特殊要求" />
                  <button type="button" class="ghost-button" @click="removeOrderItemRow(index)">移除</button>
                </div>
              </div>
              <div class="inline-actions">
                <button type="button" class="ghost-button" @click="addOrderItemRow">新增菜品</button>
                <button class="primary-button" :disabled="loading || !canCreateOrders">提交订单</button>
              </div>
            </form>
          </article>

          <article class="card">
            <div class="section-head">
              <h3>订单说明</h3>
              <span>当前按主流程实现</span>
            </div>
            <div class="stack-list">
              <div class="note-row">
                <strong>创建订单</strong>
                <p>下单时即校验原料库存，并预扣对应用量，避免后续超卖。</p>
              </div>
              <div class="note-row">
                <strong>后厨看板</strong>
                <p>后厨负责待制作、制作中、已出餐；服务员在订单页确认菜品已上桌。</p>
              </div>
              <div class="note-row">
                <strong>前台结账</strong>
                <p>前台负责收款；订单全部上桌且已支付后进入待清桌，再由服务员记录光盘情况并释放桌台。</p>
              </div>
            </div>
          </article>
        </div>

        <div class="filter-bar">
          <button
            v-for="filter in ORDER_FILTERS"
            :key="filter.value"
            type="button"
            class="filter-chip"
            :class="{ active: orderStatusFilter === filter.value }"
            @click="orderStatusFilter = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>

        <p v-if="!filteredOrders.length" class="empty">当前筛选下没有订单，已完成订单请切换到“今日已完成”或“全部历史”。</p>

        <div class="order-summary-grid">
          <button
            v-for="order in filteredOrders"
            :key="order.id"
            type="button"
            class="mini-order-card"
            :class="{ active: selectedOrderId === order.id, priority: order.priorityLevel > 0 }"
            @click="selectOrder(order)"
          >
            <span class="mini-order-no">{{ order.orderNo }}</span>
            <strong>{{ order.tableName }}</strong>
            <span>{{ formatShortTime(order.createTime) }} · {{ orderItemCount(order) }} 份菜</span>
            <span class="badge" :class="formatBadge(ORDER_STATUS, order.orderStatus).className">
              {{ formatBadge(ORDER_STATUS, order.orderStatus).text }}
            </span>
          </button>
        </div>

        <teleport to="body">
          <div v-if="selectedOrder" class="modal-backdrop" @click.self="closeOrderDetail">
            <article class="order-card order-detail-modal">
              <div class="order-head modal-head">
                <div>
                  <p class="eyebrow">{{ selectedOrder.orderNo }}</p>
                  <h3>{{ selectedOrder.tableName }}</h3>
                </div>
                <button type="button" class="ghost-button" @click="closeOrderDetail">关闭</button>
              </div>

              <div class="order-badges">
                <span class="badge" :class="formatBadge(ORDER_STATUS, selectedOrder.orderStatus).className">
                  {{ formatBadge(ORDER_STATUS, selectedOrder.orderStatus).text }}
                </span>
                <span v-if="selectedOrder.priorityLevel > 0" class="badge danger">
                  提权 {{ selectedOrder.priorityLevel }} 级
                </span>
                <span v-if="orderPendingCount(selectedOrder)" class="badge neutral">
                  {{ orderPendingCount(selectedOrder) }} 项未上桌
                </span>
              </div>

              <p class="recipe-meta">
                {{ ORDER_SOURCES.find((item) => item.value === selectedOrder.source)?.label || selectedOrder.source }} · 创建于
                {{ formatDateTime(selectedOrder.createTime) }}
              </p>

              <div v-if="selectedOrder.remark || selectedOrder.customerNote" class="order-notes">
                <div v-if="selectedOrder.remark" class="note-row">
                  <strong>订单备注</strong>
                  <p>{{ selectedOrder.remark }}</p>
                </div>
                <div v-if="selectedOrder.customerNote" class="note-row">
                  <strong>顾客备注</strong>
                  <p>{{ selectedOrder.customerNote }}</p>
                </div>
              </div>

              <div class="order-items">
                <div v-for="item in selectedOrder.items" :key="item.id" class="order-item-line">
                  <div>
                    <strong>{{ item.dishName }} × {{ item.quantity }}</strong>
                    <p>
                      {{ formatMoney(item.priceSnapshot) }}
                      <span v-if="item.specialRequest"> · {{ item.specialRequest }}</span>
                      <span v-if="item.urgeCount"> · 催单 {{ item.urgeCount }} 次</span>
                    </p>
                  </div>
                  <div class="actions">
                    <span class="badge" :class="formatBadge(ITEM_STATUS, item.itemStatus).className">
                      {{ formatBadge(ITEM_STATUS, item.itemStatus).text }}
                    </span>
                    <button
                      v-if="item.itemStatus === 'READY'"
                      class="primary-button secondary"
                      :disabled="loading || !canServeDish"
                      @click="serveKitchenItem(item.id)"
                    >
                      确认上桌
                    </button>
                    <button
                      class="ghost-button"
                      :disabled="item.itemStatus === 'SERVED' || loading || !canUrgeOrders"
                      @click="urgeItem(item.id)"
                    >
                      催单
                    </button>
                  </div>
                </div>
              </div>

              <div class="order-summary">
                <span>总额 {{ formatMoney(selectedOrder.totalAmount) }}</span>
                <span>优惠 {{ formatMoney(selectedOrder.discountAmount) }}</span>
                <strong>应收 {{ formatMoney(selectedOrder.finalAmount) }}</strong>
              </div>

              <div v-if="canPayOrders && !selectedOrder.paidTime" class="form-grid two-up compact-grid">
                <label>
                  支付方式
                  <select v-model="paymentDrafts[selectedOrder.id].paymentMethod">
                    <option v-for="item in PAY_OPTIONS" :key="item" :value="item">
                      {{ item }}
                    </option>
                  </select>
                </label>
                <label>
                  优惠金额
                  <input v-model="paymentDrafts[selectedOrder.id].discountAmount" type="number" min="0" step="0.01" />
                </label>
              </div>

              <div v-if="canPrioritizeOrders && selectedOrder.orderStatus !== 'COMPLETED'" class="form-grid two-up compact-grid">
                <label>
                  提权等级
                  <select v-model="priorityDrafts[selectedOrder.id].priorityLevel">
                    <option :value="1">1 级</option>
                    <option :value="2">2 级</option>
                    <option :value="3">3 级</option>
                  </select>
                </label>
                <label>
                  提权原因
                  <input v-model="priorityDrafts[selectedOrder.id].priorityReason" type="text" placeholder="VIP / 催单补偿" />
                </label>
              </div>

              <div class="actions" v-if="canPayOrders || canPrioritizeOrders">
                <button
                  v-if="canPayOrders"
                  class="primary-button secondary"
                  :disabled="selectedOrder.orderStatus === 'COMPLETED' || loading || !canPayOrders || !!selectedOrder.paidTime"
                  @click="payOrder(selectedOrder)"
                >
                  {{ selectedOrder.paidTime ? "已收款" : "收款" }}
                </button>
                <button
                  v-if="canPrioritizeOrders"
                  class="ghost-button"
                  :disabled="loading || !canPrioritizeOrders || selectedOrder.orderStatus === 'COMPLETED'"
                  @click="prioritizeOrder(selectedOrder)"
                >
                  提权
                </button>
              </div>

              <div v-if="selectedOrder.orderStatus === 'WAITING_CLEAR' || selectedOrder.orderStatus === 'COMPLETED'" class="waste-panel">
                <div class="section-head compact-head">
                  <h3>清桌光盘记录</h3>
                  <span>{{ selectedOrder.orderStatus === "WAITING_CLEAR" ? "待清桌" : "已完成清桌" }}</span>
                </div>
                <div class="waste-list">
                  <div
                    v-for="item in selectedOrder.items.filter((entry) => entry.itemStatus === 'SERVED')"
                    :key="`waste-${item.id}`"
                    class="waste-row"
                  >
                    <div>
                      <strong>{{ item.dishName }} × {{ item.quantity }}</strong>
                      <p v-if="item.wasteLevel">
                        已记录：{{ formatBadge(WASTE_LEVELS, item.wasteLevel).text }}
                        <span v-if="item.wasteNote"> · {{ item.wasteNote }}</span>
                      </p>
                    </div>
                    <select v-model="wasteDrafts[selectedOrder.id][item.id].wasteLevel" :disabled="!canRecordWaste">
                      <option value="CLEAN">光盘</option>
                      <option value="LEFTOVER_SOME">少量剩余</option>
                      <option value="LEFTOVER_MUCH">较多剩余</option>
                    </select>
                    <input
                      v-model="wasteDrafts[selectedOrder.id][item.id].note"
                      :disabled="!canRecordWaste"
                      type="text"
                      placeholder="备注，可留空"
                    />
                  </div>
                </div>
                <button
                  class="primary-button secondary"
                  :disabled="loading || !canRecordWaste || selectedOrder.orderStatus === 'COMPLETED'"
                  @click="submitWasteRecords(selectedOrder)"
                >
                  {{ selectedOrder.orderStatus === "COMPLETED" ? "光盘记录已完成" : "提交光盘记录并清桌" }}
                </button>
              </div>
            </article>
          </div>
        </teleport>

        <article v-if="filteredOrders.length && !selectedOrder" class="card empty-detail-card">
          <h3>点击小卡片查看详情</h3>
          <p class="empty">订单详情会以小窗口打开，不占用列表空间。</p>
        </article>
      </section>

      <section v-show="activeSection === 'kitchen'" class="panel">
        <div class="kitchen-grid">
          <article
            v-for="item in kitchenQueue"
            :key="item.itemId"
            class="kitchen-card"
            :class="{ priority: item.priorityLevel > 0 }"
          >
            <div class="order-head">
              <div>
                <p class="eyebrow">{{ item.orderNo }}</p>
                <h3>{{ item.dishName }} × {{ item.quantity }}</h3>
              </div>
              <span class="badge" :class="formatBadge(ITEM_STATUS, item.itemStatus).className">
                {{ formatBadge(ITEM_STATUS, item.itemStatus).text }}
              </span>
            </div>
            <p class="recipe-meta">{{ item.tableName }} · 等待 {{ item.waitMinutes }} 分钟</p>
            <p class="recipe-steps">
              {{ item.specialRequest || "无特殊要求" }}
            </p>
            <div class="tag-list">
              <span v-if="item.priorityLevel > 0" class="tag high-tag">
                提权 {{ item.priorityLevel }} 级 · {{ item.priorityReason || "未填原因" }}
              </span>
              <span class="tag">催单 {{ item.urgeCount }} 次</span>
            </div>
            <div class="actions recipe-actions">
              <button
                class="ghost-button"
                :disabled="item.itemStatus !== 'PENDING' || loading || !canStartCooking"
                @click="startKitchenItem(item.itemId)"
              >
                开始制作
              </button>
              <button
                class="primary-button secondary"
                :disabled="item.itemStatus !== 'COOKING' || loading || !canStartCooking"
                @click="readyKitchenItem(item.itemId)"
              >
                标记已出餐
              </button>
            </div>
          </article>
        </div>
      </section>

      <section v-show="activeSection === 'analytics'" class="panel">
        <template v-if="canViewAnalytics">
          <div class="card-grid">
            <article class="metric-card">
              <span>累计营收</span>
              <strong>{{ formatMoney(analytics.totalRevenue) }}</strong>
            </article>
            <article class="metric-card">
              <span>已结单订单</span>
              <strong>{{ analytics.completedOrders }}</strong>
            </article>
            <article class="metric-card">
              <span>已上桌菜品</span>
              <strong>{{ analytics.servedDishItems }}</strong>
            </article>
            <article class="metric-card">
              <span>已记录光盘项</span>
              <strong>{{ analytics.wasteRecordedItems }}</strong>
            </article>
            <article class="metric-card">
              <span>浪费率</span>
              <strong>{{ Number(analytics.wasteRate || 0).toFixed(2) }}%</strong>
            </article>
          </div>

          <div class="two-column">
            <article class="card table-card">
              <div class="section-head">
                <h3>热销菜品</h3>
                <span>按已上桌数量排序</span>
              </div>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>菜品</th>
                      <th>销量</th>
                      <th>光盘记录数</th>
                      <th>浪费率</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in analytics.topSellingDishes" :key="`top-${item.dishName}`">
                      <td>{{ item.dishName }}</td>
                      <td>{{ item.soldQuantity }}</td>
                      <td>{{ item.wasteRecordedCount }}</td>
                      <td>{{ Number(item.wasteRate || 0).toFixed(2) }}%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>

            <article class="card table-card">
              <div class="section-head">
                <h3>高浪费菜品</h3>
                <span>按浪费率排序</span>
              </div>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>菜品</th>
                      <th>浪费份数</th>
                      <th>光盘记录数</th>
                      <th>浪费率</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in analytics.highWasteDishes" :key="`waste-${item.dishName}`">
                      <td>{{ item.dishName }}</td>
                      <td>{{ item.wasteDishCount }}</td>
                      <td>{{ item.wasteRecordedCount }}</td>
                      <td>{{ Number(item.wasteRate || 0).toFixed(2) }}%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </article>
          </div>

          <article class="card">
            <div class="section-head">
              <h3>低库存预警</h3>
              <span>{{ analytics.lowStockMaterials.length }} 项</span>
            </div>
            <div class="stack-list">
              <div v-for="item in analytics.lowStockMaterials" :key="item.materialName" class="list-row">
                <div>
                  <strong>{{ item.materialName }}</strong>
                  <p>{{ item.currentStock }} {{ item.unit }} / 预警 {{ item.warningStock }} {{ item.unit }}</p>
                </div>
                <span class="badge" :class="formatBadge(MATERIAL_STATUS, item.status).className">
                  {{ formatBadge(MATERIAL_STATUS, item.status).text }}
                </span>
              </div>
            </div>
          </article>
        </template>
        <article v-else class="card">
          <div class="section-head">
            <h3>经营分析</h3>
            <span>当前账号无权限</span>
          </div>
          <p class="empty">该模块仅向店长和经理开放。</p>
        </article>
      </section>
    </main>
  </div>
</template>
