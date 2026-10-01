// ========================================
// BACKEND URL
// ========================================
const API_BASE_URL = "/api";

// ========================================
// ELEMENTS
// ========================================
const authCard = document.getElementById("authCard");
const authHeading = document.getElementById("authHeading");
const authSubheading = document.getElementById("authSubheading");
const authRoleField = document.getElementById("authRoleField");
const authNameField = document.getElementById("authNameField");
const authPhoneField = document.getElementById("authPhoneField");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authName = document.getElementById("authName");
const authPhone = document.getElementById("authPhone");
const authToggle = document.getElementById("authToggle");
const authSubmit = document.getElementById("authSubmit");
const authMessage = document.getElementById("authMessage");

const userBar = document.getElementById("userBar");
const userName = document.getElementById("userName");
const userRoleBadge = document.getElementById("userRoleBadge");
const logoutBtn = document.getElementById("logoutBtn");
const liveIndicator = document.getElementById("live");
const avatarInitial = document.getElementById("avatarInitial");

const appContent = document.getElementById("appContent");
const postCard = document.getElementById("postCard");
const rewardsCard = document.getElementById("rewardsCard");
const impactCard = document.getElementById("impactCard");
const donorListingsCard = document.getElementById("donorListingsCard");
const receiverSection = document.getElementById("receiverSection");
const expiringBanner = document.getElementById("expiringBanner");
const expiringScroll = document.getElementById("expiringScroll");
const adminCard = document.getElementById("adminCard");
const adminNav = document.getElementById("adminNav");
const notificationBtn = document.getElementById("notificationBtn");
const notificationPanel = document.getElementById("notificationPanel");
const notificationList = document.getElementById("notificationList");
const notificationCount = document.getElementById("notificationCount");
let notifications = [];

const foodForm = document.getElementById("foodForm");
const submitButton = document.getElementById("submitButton");
const locationStatus = document.getElementById("locationStatus");
const locationButton = document.getElementById("locationButton");
const message = document.getElementById("message");
const listingContainer = document.getElementById("listingContainer");
const donorListingContainer = document.getElementById("donorListingContainer");

const expiryDay = document.getElementById("expiryDay");
const expiryMonth = document.getElementById("expiryMonth");
const expiryTime = document.getElementById("expiryTime");

let authMode = "login";
let pickupLatitude = null;
let pickupLongitude = null;
let receiverFilter = "AVAILABLE";
let stompClient = null;
let allListingsCache = [];
let nearbyListingsCache = [];

// ========================================
// CURRENT USER
// ========================================
function getUser() {
  const raw = localStorage.getItem("user");
  return raw ? JSON.parse(raw) : null;
}
function getToken() {
  return localStorage.getItem("token");
}
function authHeaders(extra) {
  const h = Object.assign({}, extra || {});
  const t = getToken();
  if (t) h["Authorization"] = "Bearer " + t;
  return h;
}
function initialOf(nameOrEmail) {
  const s = (nameOrEmail || "U").trim();
  return s.charAt(0).toUpperCase();
}

// ========================================
// AUTH
// ========================================
function setAuthMode(mode) {
  authMode = mode;
  const isLogin = mode === "login";
  authHeading.textContent = isLogin ? "Log in" : "Create account";
  authSubheading.textContent = isLogin
    ? "Log in to post or claim food. New here? Register below."
    : "Fill your details to create an account.";
  authRoleField.hidden = isLogin;
  authNameField.hidden = isLogin;
  authPhoneField.hidden = isLogin;
  authSubmit.textContent = isLogin ? "Log in" : "Register";
  authToggle.textContent = isLogin ? "New here? Register" : "Already have an account? Log in";
  authMessage.textContent = "";
  authMessage.className = "msg";
}

authToggle.addEventListener("click", () => {
  setAuthMode(authMode === "login" ? "register" : "login");
});

authSubmit.addEventListener("click", async () => {
  const email = authEmail.value.trim();
  const password = authPassword.value;

  if (!email || !password) {
    authMessage.textContent = "❌ Please enter email and password.";
    authMessage.className = "msg error";
    return;
  }

  const body = { email, password };

  if (authMode === "register") {
    body.name = authName.value.trim();
    body.phone = authPhone.value.trim();
    const selectedRole = document.querySelector('input[name="authRole"]:checked');
    body.role = selectedRole ? selectedRole.value : "DONOR";

    if (!body.name) {
      authMessage.textContent = "❌ Please enter your name.";
      authMessage.className = "msg error";
      return;
    }
  }

  try {
    authSubmit.disabled = true;
    authSubmit.textContent = authMode === "login" ? "Logging in..." : "Creating...";

    const res = await fetch(`${API_BASE_URL}/auth/${authMode}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.message || "Something went wrong");

    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify({
      id: data.id, name: data.name, email: data.email, role: data.role
    }));

    authPassword.value = "";
    enterApp();

  } catch (err) {
    console.error(err);
    authMessage.textContent = "❌ " + err.message;
    authMessage.className = "msg error";
  } finally {
    authSubmit.disabled = false;
    authSubmit.textContent = authMode === "login" ? "Log in" : "Register";
  }
});

// ========================================
// LOGOUT
// ========================================
logoutBtn.addEventListener("click", () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  if (stompClient) { stompClient.deactivate(); stompClient = null; }
  leaveApp();
});

// ========================================
// APP ENTRY / EXIT
// ========================================
function enterApp() {
  const user = getUser();
  if (!user) { leaveApp(); return; }

  authCard.hidden = true;
  appContent.hidden = false;
  userBar.hidden = false;

  userName.textContent = user.name || user.email;
  userRoleBadge.textContent = user.role;
  if (avatarInitial) avatarInitial.textContent = initialOf(user.name || user.email);

  const profileName = document.getElementById("profileName");
  const profileEmail = document.getElementById("profileEmail");
  const profileRole = document.getElementById("profileRole");
  const profileAvatarInitial = document.getElementById("profileAvatarInitial");
  if (profileName) profileName.textContent = user.name || "User";
  if (profileEmail) profileEmail.textContent = user.email || "";
  if (profileRole) profileRole.textContent = user.role || "USER";
  if (profileAvatarInitial) profileAvatarInitial.textContent = initialOf(user.name || user.email);

  if (user.role === "DONOR") {
    postCard.hidden = false;
    impactCard.hidden = false;
    rewardsCard.hidden = false;
    donorListingsCard.hidden = false;
    receiverSection.hidden = true;
    expiringBanner.hidden = true;
    loadDonorScreen();
  } else if (user.role === "RECEIVER") {
    postCard.hidden = true;
    impactCard.hidden = true;
    rewardsCard.hidden = true;
    donorListingsCard.hidden = true;
    receiverSection.hidden = false;
    expiringBanner.hidden = false;
    loadReceiverScreen();
  } else if (user.role === "ADMIN") {
    postCard.hidden = true; impactCard.hidden = true; rewardsCard.hidden = true;
    donorListingsCard.hidden = true; receiverSection.hidden = true; expiringBanner.hidden = true;
    adminCard.hidden = false; adminNav.hidden = false;
    loadAdminDashboard();
  }

  if (user.role !== "ADMIN") adminNav.hidden = true;
  updateSubmitState();
  connectWebSocket();
}

function leaveApp() {
  authCard.hidden = false;
  appContent.hidden = true;
  userBar.hidden = true;
  postCard.hidden = true;
  impactCard.hidden = true;
  rewardsCard.hidden = true;
  donorListingsCard.hidden = true;
  receiverSection.hidden = true;
  if (adminCard) adminCard.hidden = true;
  if (adminNav) adminNav.hidden = true;
  expiringBanner.hidden = true;
  setAuthMode("login");
  updateSubmitState();
}

// ========================================
// PAGE LOAD
// ========================================
document.addEventListener("DOMContentLoaded", () => {
  fillDays();
  getCurrentLocation();

  if (getUser() && getToken()) enterApp();
  else leaveApp();

  setupReceiverFilters();
if (notificationBtn) notificationBtn.addEventListener("click", () => { notificationPanel.hidden = !notificationPanel.hidden; });
const closeNotifications = document.getElementById("closeNotifications");
if (closeNotifications) closeNotifications.addEventListener("click", () => notificationPanel.hidden = true);
});

// ========================================
// EXPIRY HELPERS
// ========================================
function fillDays() {
  if (!expiryDay) return;
  for (let day = 1; day <= 31; day++) {
    const opt = document.createElement("option");
    opt.value = day; opt.textContent = day;
    expiryDay.appendChild(opt);
  }
}

function getExpiryDateTime() {
  const day = Number(expiryDay.value);
  const month = Number(expiryMonth.value);
  const time = expiryTime.value;
  if (!day || !month || !time) return null;
  const year = new Date().getFullYear();
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}T${time}`;
}

function validateExpiry() {
  const day = Number(expiryDay.value);
  const month = Number(expiryMonth.value);
  const time = expiryTime.value;

  if (!day || !month || !time) {
    showMessage("Please select expiry date and time.", "error");
    return false;
  }

  const year = new Date().getFullYear();
  const parts = time.split(":");
  const selected = new Date(year, month - 1, day, Number(parts[0]), Number(parts[1]));

  if (selected.getMonth() !== month - 1 || selected.getDate() !== day) {
    showMessage("Please select a valid date.", "error");
    return false;
  }
  if (selected <= new Date()) {
    showMessage("Best before time must be in the future.", "error");
    return false;
  }
  return true;
}

// ========================================
// GEOLOCATION
// ========================================
function getCurrentLocation() {
  pickupLatitude = null;
  pickupLongitude = null;
  if (!locationStatus) return;

  locationStatus.textContent = "📍 Detecting your location...";
  locationStatus.className = "location-box";
  updateSubmitState();

  if (!navigator.geolocation) {
    locationStatus.textContent = "❌ Your browser does not support location.";
    locationStatus.classList.add("err");
    return;
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      pickupLatitude = position.coords.latitude;
      pickupLongitude = position.coords.longitude;
      locationStatus.textContent = "📍 Location detected successfully";
      locationStatus.className = "location-box";
      updateSubmitState();
      const currentUser = getUser();
      if (currentUser && currentUser.role === "RECEIVER") {
        loadNearbyListings();
      }
    },
    (error) => {
      console.error("Location error:", error);
      pickupLatitude = null;
      pickupLongitude = null;
      locationStatus.textContent = "❌ Please allow location access to post food.";
      locationStatus.className = "location-box err";
      updateSubmitState();
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  );
}
locationButton.addEventListener("click", getCurrentLocation);

function updateSubmitState() {
  if (!submitButton) return;
  const user = getUser();
  submitButton.disabled = !(user && user.role === "DONOR" && pickupLatitude !== null);
}

// ========================================
// POST FOOD
// ========================================
foodForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  if (!getToken()) {
    showMessage("Please log in as a donor first.", "error");
    return;
  }
  if (pickupLatitude === null || pickupLongitude === null) {
    showMessage("Please allow location access first.", "error");
    return;
  }
  if (!validateExpiry()) return;

  const selectedFoodType = document.querySelector('input[name="foodType"]:checked');

  const data = {
    title: document.getElementById("title").value.trim(),
    description: document.getElementById("description").value.trim(),
    imageUrl: document.getElementById("imageUrl")?.value.trim() || null,
    servings: Number(document.getElementById("servings").value),
    pickupLocation: document.getElementById("pickupLocation").value.trim(),
    pickupLatitude,
    pickupLongitude,
    foodType: selectedFoodType ? selectedFoodType.value : null,
    expiresAt: getExpiryDateTime()
  };

  try {
    submitButton.disabled = true;
    submitButton.textContent = "Posting...";

    const res = await fetch(`${API_BASE_URL}/listings`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify(data)
    });

    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}));
      throw new Error(errBody.message || "Failed to post food");
    }

    await res.json().catch(() => null);
    showMessage("Food posted successfully! 🎉", "success");
    foodForm.reset();
    getCurrentLocation();
    await loadDonorScreen();

  } catch (err) {
    console.error(err);
    showMessage("❌ " + err.message, "error");
  } finally {
    submitButton.textContent = "Post food →";
    updateSubmitState();
  }
});

function showMessage(text, type) {
  message.textContent = text;
  message.className = "msg " + type;
}

// ========================================
// DONOR SCREEN
// ========================================
async function loadDonorScreen() {
  await Promise.all([loadDonorStats(), loadRewardsCatalog(), loadRewardHistory(), loadDonorListings()]);
}

async function loadDonorStats() {
  try {
    const res = await fetch(`${API_BASE_URL}/donor/stats`, { headers: authHeaders() });
    if (!res.ok) throw new Error("Unable to load stats");
    const s = await res.json();
    document.getElementById("statPoints").textContent = s.points ?? 0;
    document.getElementById("statPosted").textContent = s.totalPosted ?? 0;
    document.getElementById("statClaimed").textContent = s.totalClaimed ?? 0;
    document.getElementById("statPickedUp").textContent = s.totalPickedUp ?? 0;
    document.getElementById("statExpired").textContent = s.totalExpired ?? 0;
    document.getElementById("statKgSaved").textContent = Number(s.estimatedKgSaved ?? 0).toFixed(1);
  } catch (err) {
    console.error(err);
  }
}

async function loadRewardsCatalog() {
  const container = document.getElementById("rewardsContainer");
  try {
    const res = await fetch(`${API_BASE_URL}/rewards`);
    if (!res.ok) throw new Error("Unable to load rewards");
    const rewards = await res.json();

    if (!rewards.length) {
      container.innerHTML = `<div class="empty-card">No rewards available right now.</div>`;
      return;
    }

    const myPoints = Number(document.getElementById("statPoints").textContent || 0);
    const icons = ["🥉", "🎟️", "🥈", "📜", "🥇"];

    container.innerHTML = rewards.map((r, index) => `
      <div class="reward-item">
        <div class="reward-icon">${icons[index % icons.length]}</div>
        <div class="reward-info">
          <div class="reward-name">${escapeHtml(r.name)}</div>
          <div class="reward-desc">${escapeHtml(r.description || "")}</div>
        </div>
        <span class="reward-points">${r.requiredPoints} pts</span>
        <button class="reward-redeem" ${myPoints < r.requiredPoints ? "disabled" : ""} onclick="redeemReward(${r.id})">
          ${myPoints < r.requiredPoints ? "Locked" : "Redeem"}
        </button>
      </div>
    `).join("");

  } catch (err) {
    console.error(err);
    container.innerHTML = `<div class="empty-card">❌ Unable to load rewards.</div>`;
  }
}

async function loadRewardHistory() {
  const container = document.getElementById("rewardHistory");
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE_URL}/rewards/mine`, {
      headers: authHeaders()
    });

    if (!res.ok) {
      if (res.status === 401 || res.status === 403) {
        container.innerHTML = `<div class="notification-empty">Sign in as a donor to view redeemed rewards.</div>`;
        return;
      }
      throw new Error("Unable to load reward history");
    }

    const history = await res.json();

    if (!Array.isArray(history) || history.length === 0) {
      container.innerHTML = `
        <div class="notification-empty">
          No redeemed rewards yet. Keep sharing food to earn points! 🎁
        </div>`;
      return;
    }

    container.innerHTML = history.map(item => `
      <div class="reward-history-item">
        <div>
          <strong>${escapeHtml(item.rewardName || "Reward")}</strong>
          <span>${item.redeemedAt ? formatDateTime(item.redeemedAt) : "Redeemed"}</span>
        </div>
        <span class="reward-code">${escapeHtml(item.couponCode || "—")}</span>
      </div>
    `).join("");
  } catch (err) {
    console.error("Reward history error:", err);
    container.innerHTML = `
      <div class="notification-empty">Unable to load reward history.</div>`;
  }
}

function formatDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString([], {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

async function redeemReward(rewardId) {
  if (!confirm("Redeem this reward? Points will be deducted.")) return;
  try {
    const res = await fetch(`${API_BASE_URL}/rewards/redeem`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ rewardId })
    });
    const result = await res.json();
    if (!res.ok) throw new Error(result.message || "Redeem failed");

    alert(`Redeemed "${result.rewardName}"!\nCoupon code: ${result.couponCode}`);
    await loadDonorStats();
    await loadRewardsCatalog();
    await loadRewardHistory();
  } catch (err) {
    alert("❌ " + err.message);
  }
}

async function loadDonorListings() {
  try {
    const res = await fetch(`${API_BASE_URL}/listings/mine`, { headers: authHeaders() });
    if (!res.ok) throw new Error("Unable to load your listings");
    const listings = await res.json();
    renderDonorListings(listings);
  } catch (err) {
    console.error(err);
    donorListingContainer.innerHTML = `<div class="empty-card">❌ Unable to load your listings.</div>`;
  }
}

function renderDonorListings(listings) {
  if (!listings.length) {
    donorListingContainer.innerHTML = `
      <div class="empty-card">
        <div style="font-size:28px;">🍽️</div>
        <strong>You haven't posted any food yet</strong>
        <p style="margin-top:6px;">Use the form above to share your first listing.</p>
      </div>`;
    return;
  }

  donorListingContainer.innerHTML = `<div class="listing-grid">${listings.map(l => {
    let actions = "";
    if (l.status === "CLAIMED") {
      actions += `<button class="small" onclick="markPickedUp(${l.id})">Mark picked up</button>`;
      actions += `<button class="small ghost" onclick="showContact(${l.id})">Contact receiver</button>`;
    }
    if (l.status === "AVAILABLE") {
      actions += `<button class="small danger" onclick="deleteListing(${l.id})">Delete</button>`;
      actions += `<span class="pickup-waiting">Waiting for receiver to claim</span>`;
    }
    return listingCardHtml(l, actions);
  }).join("")}</div>`;
}

// ========================================
// RECEIVER SCREEN
// ========================================
function setupReceiverFilters() {
  document.querySelectorAll("#tabs button").forEach(button => {
    button.addEventListener("click", () => setFilter(button.dataset.f));
  });
  const search = document.getElementById("listingSearch");
  const filter = document.getElementById("listingFilter");
  if (search) search.addEventListener("input", renderReceiverListings);
  if (filter) filter.addEventListener("change", renderReceiverListings);
}

function setFilter(f) {
  receiverFilter = f;
  document.querySelectorAll("#tabs button").forEach(button => {
    button.classList.toggle("active", button.dataset.f === f);
  });
  renderReceiverListings();
}

async function loadReceiverScreen() {
  await Promise.all([loadAllListings(), loadExpiringSoon()]);
  await loadNearbyListings();
}

async function loadNearbyListings() {
  if (pickupLatitude === null || pickupLongitude === null) {
    nearbyListingsCache = [];
    const status = document.getElementById("nearbyStatus");
    if (status) status.textContent = "Allow location access to see food within 10 km.";
    renderReceiverListings();
    return;
  }

  try {
    const params = new URLSearchParams({ latitude: pickupLatitude, longitude: pickupLongitude });
    const res = await fetch(`${API_BASE_URL}/listings/nearby?${params.toString()}`);
    if (!res.ok) throw new Error("Unable to find nearby food");
    nearbyListingsCache = await res.json();
    const status = document.getElementById("nearbyStatus");
    if (status) status.textContent = "Showing available food within 10 km of your current location.";
    renderReceiverListings();
  } catch (err) {
    console.error(err);
    nearbyListingsCache = [];
    const status = document.getElementById("nearbyStatus");
    if (status) status.textContent = "Unable to calculate nearby food.";
    renderReceiverListings();
  }
}

async function loadAllListings() {
  try {
    const res = await fetch(`${API_BASE_URL}/listings`);
    if (!res.ok) throw new Error("Unable to load listings");
    allListingsCache = await res.json();
    renderReceiverListings();
  } catch (err) {
    console.error(err);
    listingContainer.innerHTML = `<div class="empty-card">❌ Unable to load food listings.</div>`;
  }
}

async function loadExpiringSoon() {
  try {
    const res = await fetch(`${API_BASE_URL}/listings/expiring-soon`);
    if (!res.ok) throw new Error("Unable to load expiring listings");
    const items = await res.json();

    if (!items.length) {
      expiringScroll.innerHTML = `<div class="expiring-chip"><div class="t">Nothing expiring soon 🎉</div></div>`;
      return;
    }
    expiringScroll.innerHTML = items.map(l => `
      <div class="expiring-chip">
        <div class="t">${escapeHtml(l.title)}</div>
        <div class="m">${l.servings} servings &middot; expires ${formatDate(l.expiresAt)}</div>
      </div>
    `).join("");
  } catch (err) {
    console.error(err);
  }
}

function renderReceiverListings() {
  const user = getUser();
  let items = [];

  if (receiverFilter === "AVAILABLE") {
    items = nearbyListingsCache.filter(l => l.status === "AVAILABLE");
  } else if (receiverFilter === "MINE") {
    items = allListingsCache.filter(l => user && l.claimedById === user.id && l.status === "CLAIMED");
  } else if (receiverFilter === "HISTORY") {
    items = allListingsCache.filter(l => user && l.claimedById === user.id && (l.status === "PICKED_UP" || l.status === "EXPIRED"));
  }

  const search = document.getElementById("listingSearch")?.value.toLowerCase().trim() || "";
  const type = document.getElementById("listingFilter")?.value || "";

  items = items.filter(l => {
    const matchesSearch = !search
      || String(l.title || "").toLowerCase().includes(search)
      || String(l.pickupLocation || "").toLowerCase().includes(search);
    const matchesType = !type || l.foodType === type;
    return matchesSearch && matchesType;
  });

  if (!items.length) {
    listingContainer.innerHTML = `
      <div class="empty-card">
        <div style="font-size:28px;">🍽️</div>
        <strong>Nothing here right now</strong>
        <p style="margin-top:6px;">Check back soon, or try another tab.</p>
      </div>`;
    return;
  }

  listingContainer.innerHTML = `<div class="listing-grid">${items.map(l => {
    let actions = "";
    const isOwnClaim = user && l.claimedById === user.id;

    if (l.status === "AVAILABLE") {
      actions += `<button class="small" onclick="claimListing(${l.id})">Claim food</button>`;
    }
    if (l.status === "CLAIMED" && isOwnClaim) {
      actions += `<div class="pickup-status waiting">⏳ Waiting for donor to confirm pickup</div>`;
      actions += `<button class="small danger" onclick="cancelClaim(${l.id})">Cancel claim</button>`;
      actions += `<button class="small ghost" onclick="showContact(${l.id})">Contact donor</button>`;
    }
    if (l.status === "PICKED_UP" && isOwnClaim) {
      actions += `<div class="pickup-status completed">✅ Pickup confirmed by donor</div>`;
    }
    return listingCardHtml(l, actions);
  }).join("")}</div>`;
}

// ========================================
// SHARED LISTING ACTIONS
// ========================================
async function listingAction(id, action, method = "POST") {
  if (!getToken()) { alert("Please log in first."); return; }
  try {
    const res = await fetch(`${API_BASE_URL}/listings/${id}${action ? "/" + action : ""}`, {
      method, headers: authHeaders()
    });
    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}));
      throw new Error(errBody.message || "Action failed");
    }
    refreshCurrentScreen();
  } catch (err) {
    alert("❌ " + err.message);
  }
}

function claimListing(id) { if (confirm("Claim this food?")) listingAction(id, "claim"); }
function cancelClaim(id) { if (confirm("Cancel your claim? Others will be able to claim this food.")) listingAction(id, "cancel"); }
function markPickedUp(id) { listingAction(id, "pickup"); }
function deleteListing(id) { if (confirm("Delete this listing?")) listingAction(id, "", "DELETE"); }

async function showContact(id) {
  try {
    const res = await fetch(`${API_BASE_URL}/listings/${id}/contact`, { headers: authHeaders() });
    const c = await res.json();
    if (!res.ok) throw new Error(c.message || "Unable to load contact");
    alert(`${c.name}\nPhone: ${c.phone || "not provided"}`);
  } catch (err) {
    alert("❌ " + err.message);
  }
}

async function refreshCurrentScreen() {
  const user = getUser();
  if (!user) return;
  if (user.role === "DONOR") await loadDonorScreen();
  else if (user.role === "RECEIVER") await loadReceiverScreen();
  else if (user.role === "ADMIN") await loadAdminDashboard();
}

// ========================================
// LISTING CARD
// ========================================
function listingCardHtml(l, actionsHtml) {
  const emoji = l.foodType === "NON_VEG" ? "🍗" : "🥗";
  const thumb = l.imageUrl ? `<img src="${escapeHtml(l.imageUrl)}" alt="${escapeHtml(l.title)}" onerror="this.parentElement.innerHTML='${emoji}'">` : emoji;
  const distance = l.distanceKm != null ? `<span class="distance-badge">${Number(l.distanceKm).toFixed(1)} km</span>` : "";
  return `
    <div class="listing-card ${escapeHtml(l.status || "")}">
      <div class="listing-thumb">${thumb}</div>
      <div class="listing-content">
        <div class="listing-top">
          <h3>${escapeHtml(l.title)}</h3>
          <span class="food-badge">${formatFoodType(l.foodType)}</span>
        </div>
        <div class="info-row"><span class="info-icon">👥</span><span><strong>${l.servings ?? 0}</strong> servings</span></div>
        <div class="info-row"><span class="info-icon">📍</span><span>${escapeHtml(l.pickupLocation)}${distance}</span></div>
        <div class="info-row"><span class="info-icon">⏰</span><span>Expires ${formatDate(l.expiresAt)}</span></div>
        <div class="info-row"><span class="info-icon">🙋</span><span>${escapeHtml(l.donorName || "")}${l.claimedByName ? " | Claimed by " + escapeHtml(l.claimedByName) : ""}</span></div>
        <span class="status ${escapeHtml(l.status || "")}">${escapeHtml(String(l.status || "").replaceAll("_", " "))}</span>
        <div class="listing-actions">${actionsHtml}</div>
      </div>
    </div>
  `;
}

function formatFoodType(type) {
  if (type === "VEG") return "🥗 VEG";
  if (type === "NON_VEG") return "🍗 NON-VEG";
  return type || "FOOD";
}

function formatDate(date) {
  if (!date) return "-";
  return new Date(date).toLocaleString("en-IN", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value)
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#039;");
}

// ========================================
// WEBSOCKET
// ========================================
function connectWebSocket() {
  if (typeof StompJs === "undefined") {
    console.warn("STOMP library not loaded.");
    return;
  }
  if (stompClient) stompClient.deactivate();

  const protocol = location.protocol === "https:" ? "wss://" : "ws://";
  stompClient = new StompJs.Client({
    brokerURL: protocol + location.host + "/ws",
    reconnectDelay: 3000
  });

  stompClient.onConnect = () => {
    liveIndicator.classList.add("on");
    liveIndicator.innerHTML = '<span class="live-dot"></span> live';

    stompClient.subscribe("/topic/listings", msg => {
      let event = {}; try { event = JSON.parse(msg.body); } catch (_) {}
      const labels = {NEW:"New food available", CLAIMED:"Food claimed", CANCELLED:"Claim cancelled", PICKED_UP:"Pickup completed", EXPIRED:"Food listing expired", DELETED:"Listing removed"};
      const l = event.listing || {};
      pushNotification(labels[event.type] || "Listing update", l.title ? `${l.title} • ${l.servings || 0} servings` : "Food listing updated");
      refreshCurrentScreen();
      const user = getUser();
      if (user && user.role === "RECEIVER") loadExpiringSoon();
    });
  };

  stompClient.onWebSocketClose = () => {
    liveIndicator.classList.remove("on");
    liveIndicator.innerHTML = '<span class="live-dot"></span> offline';
  };

  stompClient.activate();
}
