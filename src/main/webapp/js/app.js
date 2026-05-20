// ==================== SYOS Online Store JavaScript ====================

const API_BASE = '/api';

// ==================== State ====================
let currentUser = null;
let cart = [];
let orders = [];
let products = [];

// ==================== Init ====================
document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
    // Auto-refresh products every 30 seconds for real-time updates
    setInterval(loadProducts, 30000);
});

// ==================== Products ====================
async function loadProducts() {
    try {
        const resp = await fetch(`${API_BASE}/products`);
        if (!resp.ok) throw new Error('Failed to load products');
        products = await resp.json();
        renderProducts();
    } catch (err) {
        document.getElementById('productsGrid').innerHTML =
            '<p class="empty-state">Failed to load products. Please try again.</p>';
        console.error(err);
    }
}

function renderProducts() {
    const grid = document.getElementById('productsGrid');
    if (!products || products.length === 0) {
        grid.innerHTML = '<p class="empty-state">No products available at the moment.</p>';
        return;
    }

    // Product emoji icons based on common grocery items
    const icons = ['🍚', '🫘', '🧈', '🍞', '🥛', '🧂', '🍵', '🧴', '🥤', '🍬',
                   '🍪', '🥫', '🧃', '🍜', '🫒', '🥚', '🍯', '🧀', '🍝', '🥣'];

    grid.innerHTML = products.map((p, i) => {
        const icon = icons[i % icons.length];
        const inStock = p.onlineQuantity > 0;
        const hasDiscount = p.discountPercentage > 0;

        return `
        <div class="product-card">
            <div class="product-img">${icon}</div>
            <div class="product-info">
                <div class="product-name">${escapeHtml(p.name)}</div>
                <div class="product-code">${p.code} · ${p.unit || 'pcs'}</div>
                <div class="product-price-row">
                    <div>
                        <span class="product-price">Rs. ${p.discountedPrice.toFixed(2)}</span>
                        ${hasDiscount ? `<span class="product-original-price">Rs. ${p.price.toFixed(2)}</span>` : ''}
                    </div>
                    ${hasDiscount ? `<span class="product-discount">${p.discountPercentage}% OFF</span>` : ''}
                </div>
                <div class="product-stock ${inStock ? 'in-stock' : 'out-of-stock'}">
                    ${inStock ? `✓ ${p.onlineQuantity} available` : '✕ Out of stock'}
                </div>
                <div class="product-actions">
                    <input type="number" class="qty-input" id="qty-${p.code}" value="1" min="1" 
                           max="${p.onlineQuantity}" ${!inStock ? 'disabled' : ''}>
                    <button class="btn btn-primary btn-sm" onclick="addToCart('${p.code}')" 
                            ${!inStock ? 'disabled style=\"opacity:0.5\"' : ''}>
                        Add to Cart
                    </button>
                </div>
            </div>
        </div>`;
    }).join('');
}

// ==================== Cart ====================
function addToCart(productCode) {
    const product = products.find(p => p.code === productCode);
    if (!product) return;

    const qtyInput = document.getElementById(`qty-${productCode}`);
    const qty = parseInt(qtyInput.value) || 1;

    if (qty > product.onlineQuantity) {
        showToast('Not enough stock available!', 'error');
        return;
    }

    // Check if already in cart
    const existing = cart.find(item => item.productCode === productCode);
    if (existing) {
        existing.quantity += qty;
    } else {
        cart.push({
            productCode: productCode,
            name: product.name,
            price: product.discountedPrice,
            quantity: qty
        });
    }

    updateCartUI();
    showToast(`Added ${product.name} to cart!`, 'success');
}

function removeFromCart(index) {
    cart.splice(index, 1);
    updateCartUI();
}

function updateCartUI() {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    document.getElementById('cartCount').textContent = count;
    document.getElementById('cartTotal').textContent = `Rs. ${total.toFixed(2)}`;

    const container = document.getElementById('cartItems');
    if (cart.length === 0) {
        container.innerHTML = '<p class="empty-state">Your cart is empty</p>';
        return;
    }

    container.innerHTML = cart.map((item, i) => `
        <div class="cart-item">
            <div class="cart-item-icon">🛍️</div>
            <div class="cart-item-info">
                <div class="cart-item-name">${escapeHtml(item.name)}</div>
                <div class="cart-item-detail">${item.quantity} × Rs. ${item.price.toFixed(2)}</div>
            </div>
            <div class="cart-item-price">Rs. ${(item.price * item.quantity).toFixed(2)}</div>
            <button class="cart-item-remove" onclick="removeFromCart(${i})">✕</button>
        </div>
    `).join('');
}

function toggleCart() {
    document.getElementById('cartSidebar').classList.toggle('show');
    document.getElementById('cartOverlay').classList.toggle('show');
}

// ==================== Checkout ====================
async function checkout() {
    if (cart.length === 0) {
        showToast('Your cart is empty!', 'error');
        return;
    }

    if (!currentUser) {
        showToast('Please login to place an order', 'error');
        toggleCart();
        showModal('loginModal');
        return;
    }

    const total = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);

    const payload = {
        items: cart.map(item => ({
            productCode: item.productCode,
            quantity: item.quantity
        })),
        totalAmount: total,
        customerId: currentUser.userId || currentUser.email
    };

    try {
        const resp = await fetch(`${API_BASE}/checkout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await resp.json();

        if (!resp.ok) {
            throw new Error(data.error || 'Checkout failed');
        }

        // Success!
        orders.unshift({
            serialNumber: data.serialNumber,
            date: data.billDate,
            total: data.total,
            items: data.items
        });

        cart = [];
        updateCartUI();
        toggleCart();
        loadProducts(); // Refresh stock levels

        showToast(`Order placed! Bill #${data.serialNumber}`, 'success');

    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==================== Auth ====================
async function login() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!email || !password) {
        showToast('Please fill in all fields', 'error');
        return;
    }

    try {
        const resp = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await resp.json();

        if (!resp.ok) {
            throw new Error(data.error || 'Login failed');
        }

        currentUser = data;
        document.getElementById('userName').textContent = `Welcome, ${data.name}`;
        document.getElementById('authSection').style.display = 'none';
        document.getElementById('userSection').style.display = 'flex';
        hideModal('loginModal');
        showToast(`Welcome back, ${data.name}!`, 'success');

        document.getElementById('loginEmail').value = '';
        document.getElementById('loginPassword').value = '';

    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function register() {
    const name = document.getElementById('regName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const address = document.getElementById('regAddress').value.trim();

    if (!name || !email || !password) {
        showToast('Please fill in all required fields', 'error');
        return;
    }

    try {
        const resp = await fetch(`${API_BASE}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, address })
        });

        const data = await resp.json();

        if (!resp.ok) {
            throw new Error(data.error || 'Registration failed');
        }

        hideModal('registerModal');
        showModal('loginModal');
        showToast('Account created! Please login.', 'success');

        document.getElementById('regName').value = '';
        document.getElementById('regEmail').value = '';
        document.getElementById('regPassword').value = '';
        document.getElementById('regAddress').value = '';

    } catch (err) {
        showToast(err.message, 'error');
    }
}

function logout() {
    currentUser = null;
    orders = [];
    document.getElementById('authSection').style.display = 'flex';
    document.getElementById('userSection').style.display = 'none';
    showToast('Logged out successfully', 'success');
}

// ==================== Navigation ====================
function showSection(section) {
    document.getElementById('productsSection').style.display = section === 'products' ? 'block' : 'none';
    document.getElementById('ordersSection').style.display = section === 'orders' ? 'block' : 'none';

    document.getElementById('nav-products').classList.toggle('active', section === 'products');
    document.getElementById('nav-orders').classList.toggle('active', section === 'orders');

    if (section === 'orders') renderOrders();
}

function renderOrders() {
    const container = document.getElementById('ordersList');
    if (!currentUser) {
        container.innerHTML = '<p class="empty-state">Please login to view your orders</p>';
        return;
    }

    if (orders.length === 0) {
        container.innerHTML = '<p class="empty-state">No orders yet. Start shopping!</p>';
        return;
    }

    container.innerHTML = orders.map(order => `
        <div class="order-card">
            <div class="order-header">
                <span class="order-id">Order #${order.serialNumber}</span>
                <span class="order-date">${order.date}</span>
            </div>
            <div class="order-total">Rs. ${Number(order.total).toFixed(2)}</div>
        </div>
    `).join('');
}

// ==================== Modals & Toast ====================
function showModal(id) {
    document.getElementById(id).classList.add('show');
}

function hideModal(id) {
    document.getElementById(id).classList.remove('show');
}

function showToast(message, type = '') {
    const toast = document.getElementById('toast');
    const msgEl = document.getElementById('toastMessage');
    msgEl.textContent = message;
    toast.className = 'toast show ' + type;
    setTimeout(() => { toast.className = 'toast'; }, 3000);
}

// ==================== Helpers ====================
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
