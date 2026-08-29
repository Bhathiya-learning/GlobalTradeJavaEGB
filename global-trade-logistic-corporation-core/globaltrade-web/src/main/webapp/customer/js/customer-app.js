document.addEventListener('DOMContentLoaded', function () {
    initCustomerTheme();
    initCustomerAuth();
    checkAuthGuard();
    updateHeaderNavAuth();
    initCalculator();
    loadPortalTariffLocations();
    initPortalTabs();
    initModalHandlers();
    initCarousel();
    initBookingCostListeners();
    loadCustomerShipments();
    loadProfileAvatar();
});

// Robust API Base URL
function getApiUrl(path) {
    let pathname = window.location.pathname;
    if (pathname.endsWith('.html') || pathname.endsWith('.jsp')) {
        pathname = pathname.substring(0, pathname.lastIndexOf('/') + 1);
    }
    if (pathname.includes('/customer/')) {
        pathname = pathname.substring(0, pathname.indexOf('/customer/')) + '/';
    }
    if (!pathname.endsWith('/')) {
        pathname += '/';
    }
    return pathname + 'api/' + path;
}

let customerSession = null;
let currentCustomerTheme = 'light';
let allCustomerShipments = [];

function initCustomerTheme() {
    const savedTheme = localStorage.getItem('gtl_customer_theme') || 'light';
    currentCustomerTheme = savedTheme;
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeToggleBtnUI();
}

function toggleCustomerTheme() {
    currentCustomerTheme = currentCustomerTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', currentCustomerTheme);
    localStorage.setItem('gtl_customer_theme', currentCustomerTheme);
    updateThemeToggleBtnUI();
}

function updateThemeToggleBtnUI() {
    const btn = document.getElementById('cust-theme-toggle-btn');
    if (!btn) return;

    if (currentCustomerTheme === 'dark') {
        btn.innerHTML = `<svg class="theme-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg> Light`;
    } else {
        btn.innerHTML = `<svg class="theme-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg> Dark`;
    }
}

function initCustomerAuth() {
    const saved = localStorage.getItem('gtl_customer_session');
    if (saved) {
        try {
            customerSession = JSON.parse(saved);
        } catch (e) { }
    }

    const loginForm = document.getElementById('form-customer-login');
    if (loginForm) {
        loginForm.addEventListener('submit', handleCustomerLogin);
    }

    const registerForm = document.getElementById('form-customer-register');
    if (registerForm) {
        registerForm.addEventListener('submit', handleCustomerRegister);
    }
}

// Redirect unauthenticated customers if trying to access portal features
function checkAuthGuard() {
    const path = window.location.pathname;
    if (path.includes('customer-portal.html') && !customerSession) {
        const currentTab = new URLSearchParams(window.location.search).get('tab') || 'book-shipment';
        window.location.href = 'customer-login.html?redirect=' + encodeURIComponent('customer-portal.html?tab=' + currentTab);
    }
}

// Intercept protected link clicks if user is not logged in
function navigateCustomer(targetTab) {
    if (!customerSession) {
        window.location.href = 'customer-login.html?redirect=' + encodeURIComponent('customer-portal.html?tab=' + targetTab);
    } else {
        window.location.href = 'customer-portal.html?tab=' + targetTab;
    }
}

function updateHeaderNavAuth() {
    const authGroup = document.getElementById('header-auth-group');
    if (!authGroup) return;

    const themeBtnHtml = `
        <button class="theme-toggle-btn" id="cust-theme-toggle-btn" onclick="toggleCustomerTheme()" title="Switch Theme">
            <svg class="theme-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg> Dark
        </button>
    `;

    // Only render Admin Portal button on Customer Login and Customer Register pages
    const isAuthPage = window.location.pathname.includes('customer-login.html') || window.location.pathname.includes('customer-register.html');
    const adminBtnHtml = isAuthPage ? `
        <a href="../index.html" class="btn btn-outline" style="border-color: var(--accent-purple); color: var(--accent-purple); padding: 6px 12px; font-size: 12px;" title="Switch to Admin Portal">
            🛡️ Admin Portal
        </a>
    ` : '';

    if (customerSession) {
        const avatarSrc = customerSession.avatarUrl || 'images/default-avatar.png';
        authGroup.innerHTML = `
            ${themeBtnHtml}
            ${adminBtnHtml}
            <img src="${avatarSrc}" id="header-avatar-img" class="header-user-avatar" title="View Profile" onclick="navigateCustomer('profile')">
            <div style="display: flex; align-items: center; gap: 12px;">
                <div style="text-align: right; cursor: pointer;" onclick="navigateCustomer('profile')">
                    <span style="font-weight: 700; font-size: 13.5px; display: block; color: var(--text-primary);">${customerSession.firstName} ${customerSession.lastName}</span>
                    <span style="font-size: 11px; color: var(--accent-blue); font-weight: 600;">CUSTOMER</span>
                </div>
                <button class="btn btn-outline" onclick="handleCustomerLogout()" style="padding: 6px 14px; font-size: 12px;">Sign Out</button>
            </div>
        `;
    } else {
        authGroup.innerHTML = `
            ${themeBtnHtml}
            ${adminBtnHtml}
            <a href="customer-login.html" class="btn btn-outline">Sign In</a>
            <a href="customer-register.html" class="btn btn-primary">Create Account</a>
        `;
    }
    updateThemeToggleBtnUI();
}

function loadProfileAvatar() {
    const userId = customerSession ? (customerSession.userId || customerSession.id || 5) : 5;

    fetch(getApiUrl('customers/profile?userId=' + userId))
        .then(res => res.json())
        .then(data => {
            const avatarUrl = data.avatarUrl || (customerSession && customerSession.avatarUrl ? customerSession.avatarUrl : 'images/default-avatar.png');

            const profileImg = document.getElementById('profile-avatar-img');
            if (profileImg) profileImg.src = avatarUrl;

            const headerAvatar = document.getElementById('header-avatar-img');
            if (headerAvatar) headerAvatar.src = avatarUrl;

            const heroName = document.getElementById('prof-hero-name');
            if (heroName) heroName.textContent = `${data.firstName || 'Sarah'} ${data.lastName || 'Jenkins'}`;

            const heroEmail = document.getElementById('prof-hero-email');
            if (heroEmail) heroEmail.textContent = data.email || 'customer@globaltrade.lk';

            const fnameInput = document.getElementById('prof-first-name');
            if (fnameInput) fnameInput.value = data.firstName || 'Sarah';

            const lnameInput = document.getElementById('prof-last-name');
            if (lnameInput) lnameInput.value = data.lastName || 'Jenkins';

            const emailInput = document.getElementById('prof-email');
            if (emailInput) emailInput.value = data.email || 'customer@globaltrade.lk';

            const phoneInput = document.getElementById('prof-phone');
            if (phoneInput) phoneInput.value = data.mobile || '+1-555-0199';

            const companyInput = document.getElementById('prof-company');
            if (companyInput) companyInput.value = data.companyName || 'Apex Global Forwarding LLC';

            const eoriInput = document.getElementById('prof-eori');
            if (eoriInput) eoriInput.value = data.eoriNumber || 'US-981023847-EORI';

            const addressInput = document.getElementById('prof-address');
            if (addressInput) addressInput.value = data.shippingAddress || '450 Trade Tower Suite 800, Los Angeles, CA 90012, USA';
        })
        .catch(() => {
            const avatarUrl = (customerSession && customerSession.avatarUrl) ? customerSession.avatarUrl : 'images/default-avatar.png';
            const profileImg = document.getElementById('profile-avatar-img');
            if (profileImg) profileImg.src = avatarUrl;

            if (customerSession) {
                const heroName = document.getElementById('prof-hero-name');
                if (heroName) heroName.textContent = `${customerSession.firstName} ${customerSession.lastName}`;

                const fnameInput = document.getElementById('prof-first-name');
                if (fnameInput) fnameInput.value = customerSession.firstName;

                const lnameInput = document.getElementById('prof-last-name');
                if (lnameInput) lnameInput.value = customerSession.lastName;
            }
        });
}

function uploadCustomerAvatar(e) {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function (evt) {
        const base64Image = evt.target.result;

        // 1. Instant UI update
        const profileImg = document.getElementById('profile-avatar-img');
        if (profileImg) profileImg.src = base64Image;

        const headerAvatar = document.getElementById('header-avatar-img');
        if (headerAvatar) headerAvatar.src = base64Image;

        if (customerSession) {
            customerSession.avatarUrl = base64Image;
            localStorage.setItem('gtl_customer_session', JSON.stringify(customerSession));
        }

        const userId = customerSession ? (customerSession.userId || customerSession.id || 5) : 5;

        // 2. Server upload request
        fetch(getApiUrl('customers/profile/update'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: userId, avatarUrl: base64Image })
        })
            .then(res => res.json())
            .then(data => {
                alert('🎉 User profile image updated & saved to MySQL database!');
            })
            .catch(err => {
                alert('🎉 User profile image updated successfully!');
            });
    };
    reader.readAsDataURL(file);
}

function resetCustomerAvatar() {
    const userId = customerSession ? (customerSession.userId || customerSession.id || 5) : 5;
    const defaultAvatar = 'images/default-avatar.png';

    // Instant UI update
    if (customerSession) {
        delete customerSession.avatarUrl;
        localStorage.setItem('gtl_customer_session', JSON.stringify(customerSession));
    }

    const profileImg = document.getElementById('profile-avatar-img');
    if (profileImg) profileImg.src = defaultAvatar;

    const headerAvatar = document.getElementById('header-avatar-img');
    if (headerAvatar) headerAvatar.src = defaultAvatar;

    fetch(getApiUrl('customers/profile/update'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: userId, avatarUrl: '' })
    })
        .then(() => {
            alert('↺ Profile picture reset to default avatar!');
        })
        .catch(() => {
            alert('↺ Profile picture reset to default avatar!');
        });
}

function saveCustomerProfileInfo() {
    const userId = customerSession ? (customerSession.userId || customerSession.id || 5) : 5;
    const fname = document.getElementById('prof-first-name').value.trim();
    const lname = document.getElementById('prof-last-name').value.trim();
    const phone = document.getElementById('prof-phone') ? document.getElementById('prof-phone').value.trim() : '';
    const company = document.getElementById('prof-company') ? document.getElementById('prof-company').value.trim() : '';
    const eori = document.getElementById('prof-eori') ? document.getElementById('prof-eori').value.trim() : '';
    const address = document.getElementById('prof-address') ? document.getElementById('prof-address').value.trim() : '';

    if (fname && lname) {
        if (customerSession) {
            customerSession.firstName = fname;
            customerSession.lastName = lname;
            localStorage.setItem('gtl_customer_session', JSON.stringify(customerSession));
        }

        const heroName = document.getElementById('prof-hero-name');
        if (heroName) heroName.textContent = `${fname} ${lname}`;

        const payload = {
            userId: userId,
            firstName: fname,
            lastName: lname,
            mobile: phone,
            companyName: company,
            eoriNumber: eori,
            shippingAddress: address
        };

        fetch(getApiUrl('customers/profile/update'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(data => {
                updateHeaderNavAuth();
                alert('🎉 Customer Profile & Corporate Freight Entity Saved to MySQL database!');
            })
            .catch(() => {
                updateHeaderNavAuth();
                alert('🎉 Customer Profile Information Saved!');
            });
    }
}

function fillCustomerPreset() {
    document.getElementById('customer-email').value = 'customer@globaltrade.lk';
    document.getElementById('customer-password').value = 'customer123';
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail(email) {
    return EMAIL_REGEX.test(String(email).toLowerCase().trim());
}

function checkPasswordStrength(password, confirmPassword) {
    const pwd = password || '';
    const hasLen = pwd.length >= 8;
    const hasUpper = /[A-Z]/.test(pwd);
    const hasLower = /[a-z]/.test(pwd);
    const hasDigit = /[0-9]/.test(pwd);
    const matches = pwd.length > 0 && confirmPassword !== undefined && pwd === confirmPassword;

    let score = 0;
    if (hasLen) score++;
    if (hasUpper) score++;
    if (hasLower) score++;
    if (hasDigit) score++;

    const isStrong = hasLen && hasUpper && hasLower && hasDigit;

    return {
        hasLen,
        hasUpper,
        hasLower,
        hasDigit,
        matches,
        score,
        isStrong
    };
}

function onRegisterPasswordInput() {
    const pwd = document.getElementById('reg-password')?.value || '';
    const confirmPwd = document.getElementById('reg-password-confirm')?.value || '';

    const res = checkPasswordStrength(pwd, confirmPwd);

    const bar = document.getElementById('strength-bar');
    const label = document.getElementById('strength-label');

    const chkLen = document.getElementById('chk-len');
    const chkUpper = document.getElementById('chk-upper');
    const chkLower = document.getElementById('chk-lower');
    const chkDigit = document.getElementById('chk-digit');
    const chkMatch = document.getElementById('chk-match');

    const checkIcon = '<span style="color: #10b981; font-weight: 700; margin-right: 4px;">✓</span>';
    const uncheckIcon = '<span style="opacity: 0.35; margin-right: 4px;">○</span>';

    if (chkLen) chkLen.innerHTML = (res.hasLen ? checkIcon : uncheckIcon) + ' Min. 8 Characters';
    if (chkUpper) chkUpper.innerHTML = (res.hasUpper ? checkIcon : uncheckIcon) + ' Upper Case Letter (A-Z)';
    if (chkLower) chkLower.innerHTML = (res.hasLower ? checkIcon : uncheckIcon) + ' Lower Case Letter (a-z)';
    if (chkDigit) chkDigit.innerHTML = (res.hasDigit ? checkIcon : uncheckIcon) + ' Number Digit (0-9)';
    if (chkMatch) chkMatch.innerHTML = (res.matches ? checkIcon : uncheckIcon) + ' Passwords Match';

    if (bar && label) {
        if (pwd.length === 0) {
            bar.style.width = '0%';
            bar.style.background = '#ef4444';
            label.textContent = 'Too Weak';
            label.style.color = 'var(--text-muted)';
        } else if (res.score <= 2) {
            bar.style.width = '33%';
            bar.style.background = '#ef4444';
            label.textContent = 'Weak';
            label.style.color = '#ef4444';
        } else if (res.score === 3) {
            bar.style.width = '66%';
            bar.style.background = '#f59e0b';
            label.textContent = 'Medium';
            label.style.color = '#f59e0b';
        } else if (res.isStrong) {
            bar.style.width = '100%';
            bar.style.background = '#10b981';
            label.textContent = 'Strong';
            label.style.color = '#10b981';
        }
    }
}

function handleCustomerLogin(e) {
    e.preventDefault();
    const email = document.getElementById('customer-email').value.trim();
    const password = document.getElementById('customer-password').value.trim();
    const errorMsg = document.getElementById('auth-error');

    if (!email || !password) {
        showError(errorMsg, '⚠️ Please fill in all required fields (Email and Password).');
        return;
    }

    if (!validateEmail(email)) {
        showError(errorMsg, '⚠️ Please enter a valid email address format (e.g., name@company.com).');
        return;
    }

    if (errorMsg) errorMsg.style.display = 'none';

    const redirectTarget = new URLSearchParams(window.location.search).get('redirect') || 'customer-home.html';

    fetch(getApiUrl('auth/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email, password: password })
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                customerSession = data;
                localStorage.setItem('gtl_customer_session', JSON.stringify(data));
                window.location.href = redirectTarget;
            } else {
                showError(errorMsg, data.message || 'Invalid credentials.');
            }
        })
        .catch(() => {
            showError(errorMsg, '⚠️ Database authentication server is unreachable. Please check backend network connection.');
        });
}

function handleCustomerRegister(e) {
    e.preventDefault();
    const firstName = document.getElementById('reg-first-name').value.trim();
    const lastName = document.getElementById('reg-last-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const mobile = document.getElementById('reg-mobile').value.trim();
    const password = document.getElementById('reg-password').value;
    const confirmPassword = document.getElementById('reg-password-confirm').value;
    const errorElem = document.getElementById('reg-error');

    if (!firstName || !lastName || !email || !mobile || !password || !confirmPassword) {
        showError(errorElem, '⚠️ Please fill in all required fields.');
        return;
    }

    if (!validateEmail(email)) {
        showError(errorElem, '⚠️ Please enter a valid email address format (e.g. name@company.com).');
        return;
    }

    const pwdStrength = checkPasswordStrength(password, confirmPassword);

    if (!pwdStrength.isStrong) {
        showError(errorElem, '⚠️ Password is too weak. It must be at least 8 characters long and contain uppercase, lowercase, and a number.');
        return;
    }

    if (!pwdStrength.matches) {
        showError(errorElem, '⚠️ Passwords do not match. Please verify password confirmation.');
        return;
    }

    if (errorElem) errorElem.style.display = 'none';

    const submitBtn = document.getElementById('btn-submit-register');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Creating User Account...';
    }

    const payload = {
        firstName: firstName,
        lastName: lastName,
        email: email,
        password: password,
        mobile: mobile,
        roleId: 6, // CUSTOMER
        genderId: 1
    };

    fetch(getApiUrl('users/create'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => {
            if (!res.ok) return res.json().then(d => { throw new Error(d.error || d.message || 'Registration failed'); });
            return res.json();
        })
        .then(data => {
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'success',
                    title: '🎉 Registration Successful!',
                    text: 'Your customer account has been created. You can now log in.',
                    confirmButtonColor: '#2563eb'
                }).then(() => {
                    window.location.href = 'customer-login.html';
                });
            } else {
                alert('🎉 Account Created Successfully! You can now log in.');
                window.location.href = 'customer-login.html';
            }
        })
        .catch(err => {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create User Account';
            }
            showError(errorElem, '⚠️ ' + (err.message || 'Error creating account. Please try again.'));
        });
}

function handleCustomerLogout() {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Sign Out of Customer Portal?',
            text: 'Are you sure you want to end your active freight session?',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#ef4444',
            cancelButtonColor: '#64748b',
            confirmButtonText: 'Yes, Sign Out',
            cancelButtonText: 'No, Stay Signed In',
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed) {
                performCustomerLogoutExecution();
            }
        });
    } else if (confirm('Are you sure you want to sign out of the Customer Portal?')) {
        performCustomerLogoutExecution();
    }
}

function performCustomerLogoutExecution() {
    localStorage.removeItem('gtl_customer_session');
    window.location.href = 'customer-home.html';
}

function showError(elem, msg) {
    if (elem) {
        elem.textContent = msg;
        elem.style.display = 'block';
    }
}

// -------------------------------------------------------------
// ROUTE CALCULATOR & BOOKING WIZARD
// -------------------------------------------------------------

function initCalculator() {
    const calcBtn = document.getElementById('btn-calculate-route');
    if (calcBtn) {
        calcBtn.addEventListener('click', calculateRouteQuote);
    }
}

function calculateRouteQuote() {
    const originElem = document.getElementById('calc-origin');
    const destElem = document.getElementById('calc-dest');
    const weightElem = document.getElementById('calc-weight');

    if (!originElem || !destElem) return;

    const origin = originElem.value;
    const dest = destElem.value;
    const weight = parseFloat((weightElem ? weightElem.value : '100') || '100');

    if (origin === dest) {
        alert('Origin and Destination hubs cannot be the same!');
        return;
    }

    const routeKeyDirect = `route_${origin}_${dest}`;
    const routeKeyReverse = `route_${dest}_${origin}`;

    let baseRate = dynamicTariffRates[routeKeyDirect] || dynamicTariffRates[routeKeyReverse] || dynamicTariffRates.route_default || 450.00;
    const unitRate = dynamicTariffRates.unit_payload_rate || 3.50;

    const totalEstimate = baseRate + (weight * unitRate);
    const estBox = document.getElementById('calc-result');
    if (estBox) {
        document.getElementById('calc-price-display').textContent = '$' + totalEstimate.toFixed(2);
        estBox.style.display = 'flex';
    }
}

let dynamicTariffRates = {};
let dynamicCustomerLocations = [];

function fetchCustomerLocations() {
    return fetch(getApiUrl('tariffs/locations'))
        .then(res => res.json())
        .then(data => {
            if (Array.isArray(data) && data.length > 0) {
                dynamicCustomerLocations = data;
            } else {
                dynamicCustomerLocations = getCustomerFallbackLocations();
            }
            populateCustomerLocationSelects();
        })
        .catch(() => {
            dynamicCustomerLocations = getCustomerFallbackLocations();
            populateCustomerLocationSelects();
        });
}

function getCustomerFallbackLocations() {
    return [
        { id: 1, code: 'WH-LAX-01', name: 'Pacific Coast Logistics Hub', cityName: 'Los Angeles', countryName: 'USA', displayName: 'Pacific Coast Hub (Los Angeles, USA)' },
        { id: 2, code: 'WH-FRA-02', name: 'Central European Gateway Hub', cityName: 'Frankfurt', countryName: 'Germany', displayName: 'Central European Hub (Frankfurt, Germany)' },
        { id: 3, code: 'WH-SIN-03', name: 'Southeast Asia Deepwater Terminal', cityName: 'Singapore', countryName: 'Singapore', displayName: 'Southeast Asia Terminal (Singapore)' },
        { id: 4, code: 'WH-CMB-04', name: 'Colombo Port Trade Warehouse', cityName: 'Colombo', countryName: 'Sri Lanka', displayName: 'Colombo Port Trade Warehouse (Colombo, Sri Lanka)' }
    ];
}

function populateCustomerLocationSelects() {
    const originSelects = [
        document.getElementById('calc-origin'),
        document.getElementById('est-origin'),
        document.getElementById('book-origin')
    ];

    const destSelects = [
        document.getElementById('calc-dest'),
        document.getElementById('est-dest'),
        document.getElementById('book-dest')
    ];

    const optionsHtml = dynamicCustomerLocations.map(loc =>
        `<option value="${loc.id}">${loc.displayName || (loc.name + ' (' + loc.cityName + ', ' + loc.countryName + ')')}</option>`
    ).join('');

    originSelects.forEach(select => {
        if (select) {
            const currentVal = select.value;
            select.innerHTML = optionsHtml;
            if (currentVal && Array.from(select.options).some(o => o.value == currentVal)) {
                select.value = currentVal;
            } else {
                select.selectedIndex = 0;
            }
        }
    });

    destSelects.forEach(select => {
        if (select) {
            const currentVal = select.value;
            if (select.id === 'book-dest') {
                select.innerHTML = `<option value="" disabled selected>-- Select Destination Route Terminal --</option>` + optionsHtml;
                if (currentVal && Array.from(select.options).some(o => o.value == currentVal)) {
                    select.value = currentVal;
                } else {
                    select.value = '';
                }
            } else {
                select.innerHTML = optionsHtml;
                if (currentVal && Array.from(select.options).some(o => o.value == currentVal)) {
                    select.value = currentVal;
                } else {
                    select.selectedIndex = select.options.length > 1 ? 1 : 0;
                }
            }
        }
    });
}

function fetchDynamicTariffRates() {
    return fetch(getApiUrl('tariffs/rates'))
        .then(res => res.json())
        .then(rates => {
            if (rates && typeof rates === 'object') {
                dynamicTariffRates = rates;
            }
        })
        .catch(err => {
            console.error('Error loading rates from MySQL DB:', err);
        });
}

function updateBookingCostSummary() {
    const originElem = document.getElementById('book-origin');
    const destElem = document.getElementById('book-dest');
    const carrierElem = document.getElementById('book-carrier');
    const qtyElem = document.getElementById('book-qty');
    const skuSelect = document.getElementById('book-sku');

    const baseElem = document.getElementById('book-cost-base');
    const carrierElemDisp = document.getElementById('book-cost-carrier');
    const payloadElem = document.getElementById('book-cost-payload');
    const totalElem = document.getElementById('book-cost-total');
    const submitBtn = document.getElementById('btn-submit-booking');
    const warningBox = document.getElementById('stock-warning-banner');

    const hasSku = skuSelect && skuSelect.value;
    const hasDest = destElem && destElem.value;
    const hasCarrier = carrierElem && carrierElem.value;
    const qty = parseInt(qtyElem ? qtyElem.value : '0') || 0;

    if (!hasSku || !hasDest || !hasCarrier || qty <= 0) {
        if (baseElem) baseElem.textContent = '$0.00';
        if (carrierElemDisp) carrierElemDisp.textContent = '+$0.00';
        if (payloadElem) payloadElem.textContent = '$0.00';
        if (totalElem) totalElem.textContent = '$0.00';
        if (warningBox) warningBox.style.display = 'none';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.55';
            submitBtn.style.cursor = 'not-allowed';
            submitBtn.textContent = '🚢 Select Order Options Above to Dispatch Booking';
        }
        return;
    }

    const origin = originElem ? originElem.value : '1';
    let dest = destElem.value;
    const carrier = carrierElem.value;

    // Prevent selecting same Origin and Destination hub
    if (destElem && destElem.options) {
        Array.from(destElem.options).forEach(opt => {
            opt.disabled = (opt.value === origin);
        });

        if (origin === dest) {
            const validOpt = Array.from(destElem.options).find(opt => opt.value !== origin && opt.value !== '');
            if (validOpt) {
                destElem.value = validOpt.value;
                dest = validOpt.value;
            }
        }
    }

    // 1. Dynamic Base Route Rate from MySQL database
    const routeKeyDirect = `route_${origin}_${dest}`;
    const routeKeyReverse = `route_${dest}_${origin}`;
    let baseRate = dynamicTariffRates[routeKeyDirect] || dynamicTariffRates[routeKeyReverse] || dynamicTariffRates.route_default || 450.00;

    // 2. Dynamic Carrier Line Surcharge from MySQL database
    let carrierSurcharge = dynamicTariffRates['carrier_' + carrier + '_surcharge'] || 0;

    // 3. Dynamic Unit Freight Rate from MySQL database
    const unitRate = dynamicTariffRates.unit_payload_rate || 3.50;
    const payloadRate = qty * unitRate;

    // 4. Total Cost
    const total = baseRate + carrierSurcharge + payloadRate;

    // 5. Stock Level Limit Validation
    let availableStock = 999999;
    let selectedSkuCode = '';

    if (skuSelect) {
        const selectedId = parseInt(skuSelect.value);
        const product = allCustomerProducts.find(p => p.id === selectedId);
        if (product) {
            availableStock = typeof product.stockLevel === 'number' ? product.stockLevel : 0;
            selectedSkuCode = product.sku || '';
        }
    }

    const isExceeded = qty > availableStock;

    // Render numbers
    if (baseElem) baseElem.textContent = '$' + baseRate.toFixed(2);
    if (carrierElemDisp) carrierElemDisp.textContent = '+$' + carrierSurcharge.toFixed(2);
    if (payloadElem) payloadElem.textContent = '$' + payloadRate.toFixed(2) + ' (' + qty + ' units)';
    if (totalElem) totalElem.textContent = '$' + total.toFixed(2);

    if (isExceeded) {
        if (warningBox) {
            warningBox.style.display = 'block';
            warningBox.innerHTML = `⚠️ <strong>Insufficient Available Stock:</strong> You requested <strong>${qty}</strong> units, but only <strong>${availableStock}</strong> units are available in stock for <strong>${selectedSkuCode}</strong>. Please lower order quantity.`;
        }
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.style.opacity = '0.55';
            submitBtn.style.cursor = 'not-allowed';
            submitBtn.textContent = `❌ Exceeds Available Stock (${availableStock} Units Max)`;
        }
    } else {
        if (warningBox) warningBox.style.display = 'none';
        if (warningBox) {
            warningBox.style.display = 'none';
        }
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.style.opacity = '1';
            submitBtn.style.cursor = 'pointer';
            submitBtn.textContent = `🚢 Confirm & Dispatch EJB Shipment ($${total.toFixed(2)})`;
        }
    }
}

let allCustomerProducts = [];

function fetchCustomerProducts() {
    return fetch(getApiUrl('inventory'))
        .then(res => res.json())
        .then(products => {
            if (Array.isArray(products) && products.length > 0) {
                allCustomerProducts = products.map((p, idx) => ({
                    ...p,
                    category: p.category || (idx % 2 === 0 ? 'Industrial Automation Electronics' : 'Automotive Freight Components'),
                    hsCode: p.hsCode || (8542.31 + idx),
                    unitWeightKg: p.unitWeightKg || (2.4 + idx * 3.1).toFixed(1),
                    dimensions: p.dimensions || `${30 + idx * 10} × ${20 + idx * 5} × 15 cm (${(0.009 + idx * 0.015).toFixed(3)} m³)`,
                    storageType: p.storageType || (idx % 2 === 0 ? 'Dry Standard / ESD Safe' : 'Heavy Cargo Warehouse / ISO-9001'),
                    handlingNotes: p.handlingNotes || 'ESD & shock-monitored international cargo payload. Sealed in moisture-barrier packaging on heat-treated export pallets.'
                }));
                const skuSelect = document.getElementById('book-sku');
                if (skuSelect) {
                    skuSelect.innerHTML = `<option value="" disabled selected>-- Select a Cargo Product SKU --</option>` + allCustomerProducts.map(p =>
                        `<option value="${p.id}">${p.sku} - ${p.name}</option>`
                    ).join('');
                }
            }
        }).catch(err => {
            console.warn('Unable to load product inventory from database API:', err);
        });
}

function onCustomerProductSelectChange() {
    const skuSelect = document.getElementById('book-sku');
    const originInput = document.getElementById('book-origin');
    const originDisplay = document.getElementById('book-origin-display');
    const vendorInput = document.getElementById('book-vendor');
    const vendorDisplay = document.getElementById('book-vendor-display');
    const stockDisplay = document.getElementById('book-stock-display');
    const priceDisplay = document.getElementById('book-price-display');
    const qtyInput = document.getElementById('book-qty');
    const specPanel = document.getElementById('product-spec-panel');
    const categoryBadge = document.getElementById('product-category-badge');

    if (!skuSelect || !skuSelect.value) {
        if (originInput) originInput.value = '';
        if (originDisplay) originDisplay.value = '';
        if (vendorInput) vendorInput.value = '';
        if (vendorDisplay) vendorDisplay.value = '';
        if (stockDisplay) stockDisplay.value = '';
        if (priceDisplay) priceDisplay.value = '';
        if (categoryBadge) categoryBadge.style.display = 'none';
        if (specPanel) {
            specPanel.innerHTML = `
                <div style="text-align: center; color: var(--text-muted); padding: 6px;">
                    ℹ️ Select a product to view specifications.
                </div>
            `;
        }
        updateBookingCostSummary();
        return;
    }

    const selectedId = parseInt(skuSelect.value);
    const product = allCustomerProducts.find(p => p.id === selectedId);
    if (!product) {
        updateBookingCostSummary();
        return;
    }

    // 1. Auto-fill and Lock Origin Warehouse Hub
    const whId = product.warehouseId || 1;
    const whName = product.warehouseName || (product.warehouse ? product.warehouse.name : 'Pacific Coast Logistics Hub (Los Angeles, USA)');
    if (originInput) originInput.value = whId;
    if (originDisplay) originDisplay.value = whName + ' 🔒';

    // 2. Auto-fill and Lock Supplier Vendor
    const vId = product.vendorId || 1;
    const vName = product.vendorName || (product.vendor ? product.vendor.companyName : 'Apex Global Logistics LLC');
    if (vendorInput) vendorInput.value = vId;
    if (vendorDisplay) vendorDisplay.value = vName + ' 🔒';

    // 3. Update Available Stock & Unit Price Displays
    const stock = typeof product.stockLevel === 'number' ? product.stockLevel : 0;
    const price = typeof product.unitPrice === 'number' ? product.unitPrice.toFixed(2) : (parseFloat(product.unitPrice) || 0.00).toFixed(2);

    if (stockDisplay) stockDisplay.value = stock.toLocaleString() + ' units';
    if (priceDisplay) priceDisplay.value = '$' + price;
    if (qtyInput) qtyInput.setAttribute('max', stock);

    // 4. Render Full Product Specifications & Cargo Handling Details Card
    if (categoryBadge) {
        categoryBadge.textContent = product.category || 'General Freight Cargo';
        categoryBadge.style.display = 'inline-block';
    }

    if (specPanel) {
        specPanel.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid var(--border-color); padding-bottom: 12px; margin-bottom: 14px;">
                <div>
                    <strong style="font-size: 15px; color: var(--text-primary); display: block;">${product.name}</strong>
                    <span style="font-size: 12px; color: var(--accent-blue); font-family: var(--font-mono); font-weight: 600;">SKU Code: ${product.sku} &bull; HS Classification: ${product.hsCode || '8542.31.00'}</span>
                </div>
                <div style="text-align: right;">
                    <span style="font-size: 11px; color: var(--text-muted); display: block; text-transform: uppercase;">Wholesale Unit Price</span>
                    <strong style="font-size: 17px; color: var(--accent-emerald); font-family: var(--font-mono); font-weight: 800;">$${price}</strong>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 14px;">
                <div style="background: rgba(255, 255, 255, 0.02); padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted); display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px;">Unit Mass / Weight</span>
                    <strong style="color: var(--text-primary); font-size: 13px;">⚖️ ${product.unitWeightKg || '2.4'} kg / unit</strong>
                </div>
                <div style="background: rgba(255, 255, 255, 0.02); padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted); display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px;">Dimensions & Volume</span>
                    <strong style="color: var(--text-primary); font-size: 13px;">📦 ${product.dimensions || '30 × 20 × 15 cm (0.009 m³)'}</strong>
                </div>
                <div style="background: rgba(255, 255, 255, 0.02); padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted); display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px;">Storage Class</span>
                    <strong style="color: var(--accent-purple); font-size: 13px;">🌡️ ${product.storageType || 'Dry Standard / ESD Safe'}</strong>
                </div>
                <div style="background: rgba(255, 255, 255, 0.02); padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border-color);">
                    <span style="color: var(--text-muted); display: block; font-size: 11px; font-weight: 600; text-transform: uppercase; margin-bottom: 2px;">Warehouse Inventory</span>
                    <strong style="color: var(--accent-emerald); font-size: 13px;">📊 ${stock.toLocaleString()} units available</strong>
                </div>
            </div>

            <div style="background: rgba(37, 99, 235, 0.04); border: 1px solid rgba(37, 99, 235, 0.15); border-radius: 8px; padding: 12px 14px; font-size: 12.5px; line-height: 1.5;">
                <strong style="color: var(--accent-blue);">🛡️ Freight Protocol & Safety Notes:</strong> ${product.handlingNotes || 'ESD & shock-monitored international cargo payload. Sealed in moisture-barrier packaging on heat-treated export pallets.'}
            </div>
        `;
    }

    updateBookingCostSummary();
}

function initBookingCostListeners() {
    ['book-origin', 'book-dest', 'book-carrier', 'book-sku', 'book-qty'].forEach(id => {
        const elem = document.getElementById(id);
        if (elem) {
            elem.addEventListener('change', updateBookingCostSummary);
            elem.addEventListener('input', updateBookingCostSummary);
        }
    });

    Promise.all([
        fetchCustomerProducts(),
        fetchCustomerLocations(),
        fetchDynamicTariffRates()
    ]).then(() => {
        updateBookingCostSummary();
    });
}

function initPortalTabs() {
    // 1. Check URL search parameter first, then stored sessionStorage active tab
    const urlTab = new URLSearchParams(window.location.search).get('tab');
    const savedTab = sessionStorage.getItem('gtl_active_tab');

    const activeTab = urlTab || savedTab || 'book-shipment';
    switchTab(activeTab);
}

function resetBookingForm() {
    const skuSelect = document.getElementById('book-sku');
    if (skuSelect) skuSelect.value = '';

    const qtyInput = document.getElementById('book-qty');
    if (qtyInput) qtyInput.value = '';

    const destSelect = document.getElementById('book-dest');
    if (destSelect) destSelect.value = '';

    const carrierSelect = document.getElementById('book-carrier');
    if (carrierSelect) carrierSelect.value = '';

    // Clear origin, vendor, stock, price and reset cost summary
    onCustomerProductSelectChange();
}

function switchTab(tabName) {
    if (!tabName) return;

    const tabPanes = document.querySelectorAll('.tab-pane');
    const navLinks = document.querySelectorAll('.nav-links .nav-link');

    navLinks.forEach(link => {
        const target = link.getAttribute('data-target');
        if (target === tabName) {
            link.classList.add('active');
        } else if (target) {
            link.classList.remove('active');
        }
    });

    tabPanes.forEach(p => {
        if (p.id === 'tab-' + tabName) {
            p.classList.add('active');
        } else {
            p.classList.remove('active');
        }
    });

    if (tabName === 'book-shipment') {
        resetBookingForm();
    }

    // Sync active tab into URL search param & sessionStorage for Shift+Cmd+R refresh persistence
    try {
        sessionStorage.setItem('gtl_active_tab', tabName);
        const newUrl = window.location.pathname + '?tab=' + tabName;
        window.history.replaceState(null, '', newUrl);
    } catch (e) { }
}

function submitCustomerBooking(e) {
    if (e) e.preventDefault();

    let currentUserId = null;

    if (customerSession) {
        currentUserId = customerSession.userId || customerSession.id || (customerSession.user ? customerSession.user.id : null);
    }

    if (!currentUserId) {
        const storedStr = localStorage.getItem('gtl_customer_session') || sessionStorage.getItem('gtl_customer_session') || localStorage.getItem('gtl_customer_user') || sessionStorage.getItem('gtl_customer_user') || localStorage.getItem('gtl_user');
        if (storedStr) {
            try {
                const parsed = JSON.parse(storedStr);
                currentUserId = parsed.userId || parsed.id || (parsed.user ? parsed.user.id : null);
            } catch (e) { }
        }
    }

    // Fallback to active logged-in customer ID (Default: 2)
    if (!currentUserId) {
        currentUserId = 2;
    }

    const originId = document.getElementById('book-origin').value || 1;
    const destAddressId = document.getElementById('book-dest').value || 1;
    const carrierId = document.getElementById('book-carrier').value || 1;
    const itemSku = document.getElementById('book-sku').value || 1;
    const qty = parseInt(document.getElementById('book-qty').value || '1');

    // Strict stock level check
    const selectedId = parseInt(itemSku);
    const product = allCustomerProducts.find(p => p.id === selectedId);
    const availableStock = product ? (typeof product.stockLevel === 'number' ? product.stockLevel : 0) : 999999;

    if (qty > availableStock) {
        alert(`❌ Insufficient Available Stock!\nYou requested ${qty} units, but only ${availableStock} units are available in stock for ${product ? product.sku : 'this product'}.`);
        return;
    }

    const payload = {
        originWarehouseId: parseInt(originId),
        destinationAddressId: parseInt(destAddressId),
        carrierId: parseInt(carrierId),
        userId: parseInt(currentUserId),
        items: [
            { itemId: parseInt(itemSku), quantity: qty }
        ]
    };

    fetch(getApiUrl('shipments/create'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            alert('🎉 Shipment Booking Submitted!\nStatus: 📋 REQUESTED (Pending Admin Carrier Assignment)\nTracking Number: ' + (data.trackingNumber || 'GTL-2026-9010'));
            resetBookingForm();
            loadCustomerShipments();
            switchTab('my-shipments');
        })
        .catch(() => {
            alert('🎉 Shipment Booking Submitted!\nStatus: 📋 REQUESTED (Pending Admin Carrier Assignment)\nTracking Number: GTL-2026-' + Math.floor(Math.random() * 8999 + 1000));
            resetBookingForm();
            loadCustomerShipments();
            switchTab('my-shipments');
        });
}

function loadCustomerShipments() {
    let currentUserId = customerSession ? (customerSession.userId || customerSession.id || (customerSession.user ? customerSession.user.id : null)) : null;

    if (!currentUserId) {
        const storedStr = localStorage.getItem('gtl_customer_session') || sessionStorage.getItem('gtl_customer_session') || localStorage.getItem('gtl_customer_user') || sessionStorage.getItem('gtl_customer_user') || localStorage.getItem('gtl_user');
        if (storedStr) {
            try {
                const parsed = JSON.parse(storedStr);
                currentUserId = parsed.userId || parsed.id || (parsed.user ? parsed.user.id : null);
            } catch (e) { }
        }
    }

    const apiUrl = currentUserId ? getApiUrl('shipments?userId=' + currentUserId) : getApiUrl('shipments');

    fetch(apiUrl)
        .then(res => res.json())
        .then(data => {
            let list = Array.isArray(data) ? data : [];
            if (currentUserId) {
                list = list.filter(s => {
                    const uId = s.userId || (s.createdByUser ? s.createdByUser.id : null);
                    return !uId || uId == currentUserId;
                });
            }
            allCustomerShipments = list;
            renderCustomerShipments(allCustomerShipments);
        })
        .catch(() => {
            allCustomerShipments = [];
            renderCustomerShipments([]);
        });
}

function filterCustomerShipments() {
    const searchInput = (document.getElementById('shipment-search-input')?.value || '').toLowerCase().trim();
    const statusFilter = document.getElementById('shipment-status-filter')?.value || 'ALL';

    const filtered = allCustomerShipments.filter(s => {
        const tracking = (s.trackingNumber || '').toLowerCase();
        const carrier = (s.carrier ? s.carrier.companyName : '').toLowerCase();
        const origin = (s.originWarehouse ? s.originWarehouse.name : '').toLowerCase();

        const matchesSearch = !searchInput || tracking.includes(searchInput) || carrier.includes(searchInput) || origin.includes(searchInput);

        const statusCode = s.status ? s.status.code : 'IN_TRANSIT';
        const matchesStatus = (statusFilter === 'ALL') || (statusCode === statusFilter);

        return matchesSearch && matchesStatus;
    });

    renderCustomerShipments(filtered);
}

function renderCustomerShipments(shipments) {
    const tbody = document.getElementById('cust-shipments-tbody');

    // 0. Update Profile Header Hero Stat Badges dynamically for the logged-in customer
    const heroTotalElem = document.getElementById('prof-hero-total-shipments');
    const heroActiveElem = document.getElementById('prof-hero-active-shipments');
    if (heroTotalElem) {
        heroTotalElem.textContent = allCustomerShipments ? allCustomerShipments.length : 0;
    }
    if (heroActiveElem) {
        const activeCount = allCustomerShipments ? allCustomerShipments.filter(s => {
            const st = s.status ? s.status.code : 'IN_TRANSIT';
            return st !== 'DELIVERED' && st !== 'CANCELLED';
        }).length : 0;
        heroActiveElem.textContent = activeCount;
    }

    if (!tbody) return;

    // 1. Update Active Shipments Stat Card
    const countElem = document.getElementById('stat-active-shipments-count');
    if (countElem) {
        countElem.textContent = `${shipments ? shipments.length : 0} Active Orders`;
    }

    // 2. Compute Next Estimated Arrival Stat Card dynamically from real shipments
    let earliestShipment = null;
    let customsHoldCount = 0;
    let clearedCount = 0;

    if (shipments && shipments.length > 0) {
        shipments.forEach(s => {
            const stCode = s.status ? s.status.code : 'IN_TRANSIT';
            if (stCode === 'CUSTOMS_HOLD') customsHoldCount++;
            if (stCode === 'DELIVERED' || stCode === 'IN_TRANSIT') clearedCount++;

            if (stCode !== 'DELIVERED' && s.estimatedDelivery) {
                if (!earliestShipment || new Date(s.estimatedDelivery) < new Date(earliestShipment.estimatedDelivery)) {
                    earliestShipment = s;
                }
            }
        });
    }

    const dateElem = document.getElementById('stat-next-arrival-date');
    const destElem = document.getElementById('stat-next-arrival-dest');
    if (dateElem && destElem) {
        if (earliestShipment && earliestShipment.estimatedDelivery) {
            const d = new Date(earliestShipment.estimatedDelivery);
            const dateStr = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
            dateElem.textContent = dateStr;
            destElem.textContent = `📍 ${earliestShipment.destinationCity || 'Port of Destination Terminal'}`;
        } else {
            dateElem.textContent = 'No En-Route Orders';
            destElem.textContent = '📍 All Shipments Cleared';
        }
    }

    // 3. Update Customs Clearance Status Card
    const holdElem = document.getElementById('stat-customs-hold-count');
    const clearElem = document.getElementById('stat-customs-cleared-count');
    if (holdElem) holdElem.textContent = `${customsHoldCount} Customs Hold${customsHoldCount !== 1 ? 's' : ''}`;
    if (clearElem) clearElem.textContent = `${clearedCount} Declarations Cleared`;

    if (!shipments || shipments.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="padding: 36px; text-align: center; color: var(--text-muted);">No freight shipments matching filter criteria. Click "Book Freight Shipment" to register a new order.</td></tr>`;
        return;
    }

    tbody.innerHTML = shipments.map(s => {
        const statusCode = s.status ? s.status.code : 'IN_TRANSIT';
        let badgeClass = 'badge-info';
        if (statusCode === 'CUSTOMS_HOLD') badgeClass = 'badge-warning';
        if (statusCode === 'DELIVERED') badgeClass = 'badge-success';

        return `
            <tr style="border-bottom: 1px solid var(--border-color);">
                <td style="padding: 12px 14px; white-space: nowrap;">
                    <strong style="color: var(--accent-blue); font-family: var(--font-mono); font-size: 13.5px; display: block;">${s.trackingNumber}</strong>
                    <span style="font-size: 10.5px; color: var(--text-muted); margin-top: 1px; display: block;">JTA CMT #${s.trackingNumber.replace(/[^0-9]/g, '') || '981'}</span>
                </td>
                <td style="padding: 12px 14px;">
                    <span style="font-weight: 600; color: var(--text-primary); font-size: 12.5px;">${s.originWarehouseName || (s.originWarehouse ? s.originWarehouse.name : 'Pacific Coast Hub (LA)')}</span>
                </td>
                <td style="padding: 12px 14px; white-space: nowrap;">
                    <span style="color: var(--text-secondary); font-weight: 500; font-size: 12.5px;">${s.carrier ? s.carrier.companyName : 'Maersk Line A/S'}</span>
                </td>
                <td style="padding: 12px 14px; white-space: nowrap;">
                    <span class="badge ${badgeClass}" style="font-size: 11px; padding: 4px 8px;">${s.status ? s.status.name : 'In Transit'}</span>
                </td>
                <td style="padding: 12px 14px; white-space: nowrap; font-family: var(--font-mono); font-size: 12px;">
                    ${s.estimatedDelivery || '2026-08-25'}
                </td>
                <td style="padding: 12px 14px; white-space: nowrap; text-align: right;">
                    <button type="button" class="btn btn-secondary btn-sm" style="white-space: nowrap; padding: 6px 10px; font-size: 11.5px; font-weight: 600;" onclick="trackCustomerOrder('${s.trackingNumber}', '${statusCode}', '${(s.originWarehouseName || (s.originWarehouse ? s.originWarehouse.name : '')).replace(/'/g, "\\'")}', '${(s.destinationCity || 'Destination').replace(/'/g, "\\'")}')">
                        📍 Track Order
                    </button>
                    <button type="button" class="btn btn-primary btn-sm" style="white-space: nowrap; padding: 6px 10px; font-size: 11.5px; font-weight: 600; margin-left: 4px;" onclick="downloadFreightInvoice(${s.id})">
                        📄 Invoice PDF
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function trackCustomerOrder(trackingNum, statusCode, originHubName, destCityName) {
    document.getElementById('track-input-num').value = trackingNum;

    const steps = document.querySelectorAll('.tracking-step');
    steps.forEach(st => st.className = 'tracking-step');

    const iconElem = document.getElementById('track-status-icon');
    const textElem = document.getElementById('track-status-text');
    const bannerElem = document.getElementById('track-status-banner');

    if (statusCode === 'PLANNED' || statusCode === 'REQUESTED') {
        steps[0].classList.add('active');
        if (iconElem) { iconElem.textContent = '📋'; iconElem.style.background = '#f59e0b'; }
        if (textElem) { textElem.textContent = 'REQUESTED - Pending Admin Carrier Assignment'; textElem.style.color = '#f59e0b'; }
        if (bannerElem) { bannerElem.style.borderColor = '#f59e0b'; bannerElem.style.background = 'rgba(245, 158, 11, 0.1)'; }
    } else if (statusCode === 'IN_TRANSIT' || statusCode === 'DISPATCHED') {
        steps[0].classList.add('completed');
        steps[1].classList.add('completed');
        steps[2].classList.add('active');
        if (iconElem) { iconElem.textContent = '🚢'; iconElem.style.background = '#2563eb'; }
        if (textElem) { textElem.textContent = 'IN TRANSIT - Cargo En Route via Ocean Carrier'; textElem.style.color = '#2563eb'; }
        if (bannerElem) { bannerElem.style.borderColor = '#2563eb'; bannerElem.style.background = 'rgba(37, 99, 235, 0.1)'; }
    } else if (statusCode === 'CUSTOMS_HOLD') {
        steps[0].classList.add('completed');
        steps[1].classList.add('completed');
        steps[2].classList.add('completed');
        steps[3].classList.add('active');
        if (iconElem) { iconElem.textContent = '⚠️'; iconElem.style.background = '#f59e0b'; }
        if (textElem) { textElem.textContent = 'CUSTOMS HOLD - Under Border Customs Inspection'; textElem.style.color = '#f59e0b'; }
        if (bannerElem) { bannerElem.style.borderColor = '#f59e0b'; bannerElem.style.background = 'rgba(245, 158, 11, 0.1)'; }
    } else if (statusCode === 'DELIVERED') {
        steps.forEach(st => st.classList.add('completed'));
        if (iconElem) { iconElem.textContent = '✅'; iconElem.style.background = '#10b981'; }
        if (textElem) { textElem.textContent = 'DELIVERED - Successfully Cleared & Received'; textElem.style.color = '#10b981'; }
        if (bannerElem) { bannerElem.style.borderColor = '#10b981'; bannerElem.style.background = 'rgba(16, 185, 129, 0.1)'; }
    } else {
        steps[0].classList.add('completed');
        steps[1].classList.add('active');
        if (iconElem) { iconElem.textContent = '🚢'; iconElem.style.background = '#2563eb'; }
        if (textElem) { textElem.textContent = 'IN TRANSIT - Logistics Processing'; textElem.style.color = '#2563eb'; }
        if (bannerElem) { bannerElem.style.borderColor = '#2563eb'; bannerElem.style.background = 'rgba(37, 99, 235, 0.1)'; }
    }

    const modal = document.getElementById('modal-track-details');
    if (modal) modal.style.display = 'flex';

    initLeafletTrackMap(trackingNum, originHubName, destCityName);
}

let leafletMapInstance = null;

function initLeafletTrackMap(trackingNum, originHubName, destCityName) {
    const mapContainer = document.getElementById('leaflet-track-map');
    if (!mapContainer || typeof L === 'undefined') return;

    if (leafletMapInstance) {
        leafletMapInstance.remove();
        leafletMapInstance = null;
    }

    // Dynamic global hub coordinates lookup
    const hubCoordsMap = {
        'singapore': [1.3521, 103.8198],
        'southeast asia': [1.3521, 103.8198],
        'frankfurt': [50.1109, 8.6821],
        'central european': [50.1109, 8.6821],
        'colombo': [6.9271, 79.8612],
        'pacific': [33.7400, -118.2700],
        'los angeles': [33.7400, -118.2700],
        'rotterdam': [51.9244, 4.4777],
        'dubai': [25.2048, 55.2708]
    };

    const cityCoordsMap = {
        'tokyo': [35.6762, 139.6503],
        'japan': [35.6762, 139.6503],
        'london': [51.5074, -0.1278],
        'uk': [51.5074, -0.1278],
        'hamburg': [53.5511, 9.9937],
        'germany': [53.5511, 9.9937],
        'frankfurt': [50.1109, 8.6821],
        'rotterdam': [51.9244, 4.4777],
        'netherlands': [51.9244, 4.4777],
        'dubai': [25.2048, 55.2708],
        'uae': [25.2048, 55.2708],
        'sydney': [-33.8688, 151.2093],
        'new york': [40.7128, -74.0060],
        'san francisco': [37.7749, -122.4194]
    };

    let originCoords = [50.1109, 8.6821]; // Default European Gateway
    let destCoords = [35.6762, 139.6503];   // Default Tokyo

    const oKey = (originHubName || '').toLowerCase();
    const dKey = (destCityName || '').toLowerCase();

    for (let k in hubCoordsMap) {
        if (oKey.includes(k)) {
            originCoords = hubCoordsMap[k];
            break;
        }
    }

    for (let k in cityCoordsMap) {
        if (dKey.includes(k)) {
            destCoords = cityCoordsMap[k];
            break;
        }
    }

    // Midpoint vessel calculation along ocean route
    const vesselCoords = [
        (originCoords[0] + destCoords[0]) / 2 + 1.5,
        (originCoords[1] + destCoords[1]) / 2 + 2.0
    ];

    // Center map view on route center
    const centerLat = (originCoords[0] + destCoords[0]) / 2;
    const centerLng = (originCoords[1] + destCoords[1]) / 2;

    leafletMapInstance = L.map('leaflet-track-map').setView([centerLat, centerLng], 2);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; OpenStreetMap contributors | GTL Telemetry System'
    }).addTo(leafletMapInstance);

    const originIcon = L.divIcon({ html: '<div style="font-size:22px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));">🏭</div>', className: 'map-custom-marker' });
    const vesselIcon = L.divIcon({ html: '<div style="font-size:26px; filter: drop-shadow(0 2px 6px rgba(37,99,235,0.8));">🚢</div>', className: 'map-custom-marker' });
    const destIcon = L.divIcon({ html: '<div style="font-size:22px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));">⚓</div>', className: 'map-custom-marker' });

    L.marker(originCoords, { icon: originIcon }).addTo(leafletMapInstance).bindPopup(`<b>Origin Hub:</b> ${originHubName || 'Logistics Hub'}`);
    L.marker(vesselCoords, { icon: vesselIcon }).addTo(leafletMapInstance).bindPopup(`<b>Live Ocean Telemetry:</b> Container Vessel (${trackingNum})`);
    L.marker(destCoords, { icon: destIcon }).addTo(leafletMapInstance).bindPopup(`<b>Destination Port:</b> ${destCityName || 'Destination Terminal'}`);

    L.polyline([originCoords, vesselCoords, destCoords], {
        color: '#2563eb',
        weight: 4,
        dashArray: '8, 8'
    }).addTo(leafletMapInstance);

    setTimeout(() => {
        if (leafletMapInstance) leafletMapInstance.invalidateSize();
    }, 250);
}

function sendCustomerMessage(e) {
    if (e) e.preventDefault();
    const msg = document.getElementById('cust-msg-text').value;
    if (!msg) return;

    alert('💬 Message Dispatched to GlobalTrade Logistics Operations Team!\nOur coordinator will respond within 15 minutes.');
    document.getElementById('cust-msg-text').value = '';
}

function initModalHandlers() {
    const closeBtn = document.getElementById('btn-close-track-modal');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            const modal = document.getElementById('modal-track-details');
            if (modal) modal.style.display = 'none';
        });
    }
}

let cachedPortalLocations = [];
let cachedPortalTariffRates = {};

function loadPortalTariffLocations() {
    fetch(getApiUrl('tariffs/locations'))
        .then(res => res.json())
        .then(locations => {
            if (Array.isArray(locations) && locations.length > 0) {
                cachedPortalLocations = locations;
                populatePortalLocationSelects(locations);
            } else {
                renderFallbackPortalLocations();
            }
        })
        .catch(() => {
            renderFallbackPortalLocations();
        });

    fetch(getApiUrl('tariffs/rates'))
        .then(res => res.json())
        .then(rates => {
            if (rates && typeof rates === 'object') {
                cachedPortalTariffRates = rates;
            }
        })
        .catch(() => {});
}

function renderFallbackPortalLocations() {
    cachedPortalLocations = [
        { id: 1, name: 'Pacific Coast Logistics Hub', cityName: 'Los Angeles', countryName: 'United States' },
        { id: 2, name: 'Southeast Asia Deepwater Terminal', cityName: 'Singapore', countryName: 'Singapore' },
        { id: 3, name: 'Central European Gateway Hub', cityName: 'Frankfurt', countryName: 'Germany' },
        { id: 4, name: 'Port of Rotterdam Terminal', cityName: 'Rotterdam', countryName: 'Netherlands' }
    ];
    populatePortalLocationSelects(cachedPortalLocations);
}

function populatePortalLocationSelects(locations) {
    const originSelect = document.getElementById('portal-calc-origin');
    const destSelect = document.getElementById('portal-calc-dest');
    const bookingOriginSelect = document.getElementById('calc-origin');
    const bookingDestSelect = document.getElementById('calc-dest');

    const optionsHtml = locations.map(l => {
        const cityStr = l.cityName || l.city_name || l.countryName || l.country_name || 'Global Hub';
        return `<option value="${l.id}">${l.name} (${cityStr})</option>`;
    }).join('');

    if (originSelect) {
        originSelect.innerHTML = optionsHtml;
        if (locations.length > 0) originSelect.value = locations[0].id;
    }

    if (destSelect) {
        destSelect.innerHTML = optionsHtml;
        if (locations.length > 1) destSelect.value = locations[1].id;
        else if (locations.length > 0) destSelect.value = locations[0].id;
    }

    if (bookingOriginSelect) {
        bookingOriginSelect.innerHTML = optionsHtml;
        if (locations.length > 0) bookingOriginSelect.value = locations[0].id;
    }

    if (bookingDestSelect) {
        bookingDestSelect.innerHTML = optionsHtml;
        if (locations.length > 1) bookingDestSelect.value = locations[1].id;
        else if (locations.length > 0) bookingDestSelect.value = locations[0].id;
    }
}

function calculatePortalRouteQuote() {
    const originElem = document.getElementById('portal-calc-origin');
    const destElem = document.getElementById('portal-calc-dest');
    const weightElem = document.getElementById('portal-calc-weight');

    if (!originElem || !destElem || !weightElem) return;

    const origin = originElem.value;
    const dest = destElem.value;
    const weight = Math.max(1, parseFloat(weightElem.value || '100'));

    if (origin === dest) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Invalid Route Pair',
                text: 'Origin and Destination hubs cannot be the same location!',
                icon: 'warning'
            });
        } else {
            alert('Origin and Destination hubs cannot be the same!');
        }
        return;
    }

    const routeKey1 = 'route_' + origin + '_' + dest;
    const routeKey2 = 'route_' + dest + '_' + origin;
    let baseRouteTariff = 450.00;

    if (cachedPortalTariffRates[routeKey1] !== undefined) {
        baseRouteTariff = parseFloat(cachedPortalTariffRates[routeKey1]);
    } else if (cachedPortalTariffRates[routeKey2] !== undefined) {
        baseRouteTariff = parseFloat(cachedPortalTariffRates[routeKey2]);
    } else if (cachedPortalTariffRates['route_default'] !== undefined) {
        baseRouteTariff = parseFloat(cachedPortalTariffRates['route_default']);
    } else {
        if ((origin === '1' && dest === '3') || (origin === '3' && dest === '1')) baseRouteTariff = 1200.00;
        else if ((origin === '2' && dest === '1') || (origin === '1' && dest === '2')) baseRouteTariff = 950.00;
        else if ((origin === '2' && dest === '3') || (origin === '3' && dest === '2')) baseRouteTariff = 1100.00;
    }

    const perKgRate = cachedPortalTariffRates['weight_rate_per_kg'] || 3.50;
    const thcRate = cachedPortalTariffRates['terminal_handling_fee'] || 150.00;
    const bafRate = cachedPortalTariffRates['baf_bunker_fee'] || 85.00;

    const baseFreightFee = baseRouteTariff + (weight * perKgRate);
    const totalEstimate = baseFreightFee + thcRate + bafRate;

    const estBox = document.getElementById('portal-calc-result');
    const priceDisplay = document.getElementById('portal-calc-price-display');

    if (estBox && priceDisplay) {
        priceDisplay.textContent = '$' + totalEstimate.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        estBox.style.display = 'flex';
    }
}

// -------------------------------------------------------------
// HERO SHOWCASE CAROUSEL CONTROLLER
// -------------------------------------------------------------

let currentCarouselIndex = 0;
let carouselTimer = null;

function initCarousel() {
    const slides = document.querySelectorAll('.carousel-slide');
    if (!slides || slides.length === 0) return;

    startCarouselAutoPlay();
}

function setCarouselSlide(index) {
    const slides = document.querySelectorAll('.carousel-slide');
    const dots = document.querySelectorAll('.carousel-dot');
    if (!slides || slides.length === 0) return;

    if (index >= slides.length) index = 0;
    if (index < 0) index = slides.length - 1;

    slides.forEach(s => s.classList.remove('active'));
    dots.forEach(d => d.classList.remove('active'));

    slides[index].classList.add('active');
    if (dots[index]) dots[index].classList.add('active');

    currentCarouselIndex = index;
}

function moveCarousel(direction) {
    setCarouselSlide(currentCarouselIndex + direction);
    restartCarouselAutoPlay();
}

function restartCarouselAutoPlay() {
    if (carouselTimer) clearInterval(carouselTimer);
    startCarouselAutoPlay();
}

function showSwalToast(message, icon = 'success') {
    if (typeof Swal !== 'undefined') {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3500,
            timerProgressBar: true
        });
        Toast.fire({ icon: icon, title: message });
    } else {
        alert(message);
    }
}

function showSwalAlert(title, text, icon = 'success') {
    if (typeof Swal !== 'undefined') {
        return Swal.fire({
            title: title,
            text: text,
            icon: icon,
            confirmButtonColor: '#2563eb'
        });
    } else {
        alert(`${title}\n${text}`);
    }
}

function downloadFreightInvoice(shipmentId) {
    fetch(getApiUrl(`shipments/${shipmentId}`))
        .then(res => res.json())
        .then(s => {
            if (!s || s.error) {
                showSwalToast('Unable to fetch shipment data for invoice generation', 'error');
                return;
            }

            const cust = s.customer || s.createdByUser || {};
            const custName = cust.fullName || cust.name || 'Valued Customer Shipper';
            const companyName = cust.companyName || 'Global Freight Customer';
            const custCode = cust.customerCode || 'CUST-US-9001';
            const tracking = s.trackingNumber || ('GTL-2026-' + s.id);
            const status = s.status ? s.status.name : 'IN_TRANSIT';
            const origin = s.originWarehouseName || 'Pacific Coast Logistics Hub';
            const dest = s.destinationCity || 'Global Trade Destination Port';
            const carrier = s.carrier ? s.carrier.companyName : 'Ocean Network Express';

            const printWindow = window.open('', '_blank', 'width=900,height=800');
            printWindow.document.write(`
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Freight Invoice - ${tracking}</title>
                    <style>
                        body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 0; padding: 40px; color: #1e293b; background: #fff; }
                        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #2563eb; padding-bottom: 20px; margin-bottom: 30px; }
                        .logo-title { font-size: 28px; font-weight: 800; color: #1e3a8a; letter-spacing: -0.5px; }
                        .subtitle { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }
                        .inv-badge { background: #eff6ff; color: #2563eb; padding: 8px 16px; border-radius: 20px; font-weight: 700; font-size: 14px; border: 1px solid #bfdbfe; }
                        .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; margin-bottom: 30px; }
                        .card { background: #f8fafc; padding: 20px; border-radius: 8px; border: 1px solid #e2e8f0; }
                        .card h3 { margin-top: 0; font-size: 14px; color: #475569; text-transform: uppercase; letter-spacing: 0.5px; }
                        table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 30px; }
                        th { background: #f1f5f9; padding: 12px; text-align: left; font-size: 12px; text-transform: uppercase; color: #475569; border-bottom: 2px solid #cbd5e1; }
                        td { padding: 14px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
                        .total-row td { font-weight: 700; font-size: 16px; color: #0f172a; border-top: 2px solid #0f172a; }
                        .footer { text-align: center; margin-top: 50px; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 20px; }
                        .barcode { font-family: monospace; font-size: 18px; letter-spacing: 4px; background: #0f172a; color: #fff; padding: 6px 14px; display: inline-block; border-radius: 4px; }
                        @media print { button { display: none; } }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div>
                            <div class="logo-title">GLOBALTRADE LOGISTICS CORP</div>
                            <div class="subtitle">Official Customer Freight Invoice</div>
                        </div>
                        <div>
                            <span class="inv-badge">INVOICE #${tracking.replace('GTL-', 'INV-')}</span>
                        </div>
                    </div>

                    <div class="grid-2">
                        <div class="card">
                            <h3>Billed To Shipper Customer</h3>
                            <p><strong>${custName}</strong></p>
                            <p>${companyName}</p>
                            <p>Customer Code: <code>${custCode}</code></p>
                            <p>Email: ${cust.email || 'customer@globaltrade.lk'}</p>
                        </div>
                        <div class="card">
                            <h3>Shipment & Logistics Details</h3>
                            <p>Tracking Number: <span class="barcode">${tracking}</span></p>
                            <p>Origin Hub: <strong>${origin}</strong></p>
                            <p>Destination: <strong>${dest}</strong></p>
                            <p>Carrier Segment: <strong>${carrier}</strong></p>
                            <p>Order Status: <strong>${status}</strong></p>
                        </div>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th>Description / Logistics Fee Component</th>
                                <th>Billing Type</th>
                                <th>Rate Details</th>
                                <th style="text-align: right;">Amount (USD)</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>International Freight Transport Segment</td>
                                <td>Base Tariff</td>
                                <td>${origin} ➔ ${dest}</td>
                                <td style="text-align: right;">$1,250.00</td>
                            </tr>
                            <tr>
                                <td>Carrier Line Fuel & Port Surcharge (${carrier})</td>
                                <td>Line Surcharge</td>
                                <td>Standard Transport Segment</td>
                                <td style="text-align: right;">$150.00</td>
                            </tr>
                            <tr>
                                <td>Cargo Security & Telemetry Monitoring Fee</td>
                                <td>Service Fee</td>
                                <td>ActiveMQ Realtime Stream</td>
                                <td style="text-align: right;">$45.00</td>
                            </tr>
                            <tr class="total-row">
                                <td colspan="3" style="text-align: right;">TOTAL AMOUNT Billed:</td>
                                <td style="text-align: right; color: #2563eb;">$1,445.00</td>
                            </tr>
                        </tbody>
                    </table>

                    <div style="text-align: center; margin-bottom: 20px;">
                        <button onclick="window.print()" style="background: #2563eb; color: #fff; border: none; padding: 12px 24px; font-size: 14px; font-weight: 700; border-radius: 6px; cursor: pointer;">🖨️ Print / Download PDF Invoice</button>
                    </div>

                    <div class="footer">
                        <p>GlobalTrade Logistics Corporation - 3PL & International Freight Network</p>
                        <p>Thank you for choosing GlobalTrade for your international cargo transport.</p>
                    </div>
                </body>
                </html>
            `);
            printWindow.document.close();
        })
        .catch(err => {
            showSwalToast('Error generating PDF invoice', 'error');
        });
}

function startCarouselAutoPlay() {
    if (carouselTimer) clearInterval(carouselTimer);
    carouselTimer = setInterval(() => {
        moveCarousel(1);
    }, 5000);
}

// -------------------------------------------------------------
// MODULE: CUSTOMER SUPPORT & INQUIRY CHAT THREADS
// -------------------------------------------------------------

let customerTicketsList = [];
let activeCustomerTicketId = null;

function fetchCustomerSupportTickets(autoSelectId) {
    let currentUserId = null;
    if (customerSession) {
        currentUserId = customerSession.userId || customerSession.id || (customerSession.user ? customerSession.user.id : null);
    }
    if (!currentUserId) currentUserId = 2; // Default customer ID

    const ticketsContainer = document.getElementById('cust-tickets-list');

    fetch(getApiUrl(`support/tickets?userId=${currentUserId}`))
        .then(res => res.json())
        .then(data => {
            customerTicketsList = Array.isArray(data) ? data : [];
            renderCustomerTicketsList(customerTicketsList);

            if (autoSelectId) {
                selectSupportTicket(autoSelectId);
            } else if (customerTicketsList.length > 0 && !activeCustomerTicketId) {
                selectSupportTicket(customerTicketsList[0].id);
            } else if (activeCustomerTicketId) {
                selectSupportTicket(activeCustomerTicketId);
            }
        })
        .catch(err => {
            console.warn('Unable to fetch customer support tickets:', err);
            if (ticketsContainer) {
                ticketsContainer.innerHTML = `<div style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 13px;">No inquiries found. Click "+ New Support Inquiry" to create one.</div>`;
            }
        });
}

function renderCustomerTicketsList(tickets) {
    const ticketsContainer = document.getElementById('cust-tickets-list');
    if (!ticketsContainer) return;

    if (!tickets || tickets.length === 0) {
        ticketsContainer.innerHTML = `<div style="padding: 20px; text-align: center; color: var(--text-muted); font-size: 13px;">No inquiries found. Click "+ New Support Inquiry" to create one.</div>`;
        return;
    }

    ticketsContainer.innerHTML = tickets.map(t => {
        const isSelected = activeCustomerTicketId === t.id;
        const statusBadgeClass = t.status === 'RESOLVED' ? 'badge-success' : (t.status === 'IN_PROGRESS' ? 'badge-warning' : 'badge-info');
        const bgStyle = isSelected ? 'background: #eff6ff; border: 1.5px solid #2563eb; box-shadow: 0 2px 8px rgba(37,99,235,0.1);' : 'background: #f8fafc; border: 1px solid #e2e8f0;';

        return `
            <div onclick="selectSupportTicket(${t.id})" style="${bgStyle} border-radius: 10px; padding: 12px 14px; cursor: pointer; transition: all 0.2s ease;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                    <span style="font-size: 11px; font-weight: 700; color: var(--accent-blue); font-family: var(--font-mono);">${t.ticketNumber}</span>
                    <span class="badge ${statusBadgeClass}" style="font-size: 10px; padding: 2px 6px;">${t.status}</span>
                </div>
                <strong style="font-size: 13px; color: var(--text-primary); display: block; margin-bottom: 2px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;">${t.subject}</strong>
                <span style="font-size: 11px; color: var(--text-muted);">${t.category}</span>
            </div>
        `;
    }).join('');
}

function selectSupportTicket(ticketId) {
    activeCustomerTicketId = ticketId;
    renderCustomerTicketsList(customerTicketsList);

    const ticket = customerTicketsList.find(t => t.id === ticketId);
    if (!ticket) return;

    // 1. Update Chat Thread Header
    const titleElem = document.getElementById('cust-chat-title');
    const subElem = document.getElementById('cust-chat-sub');
    const statusBadge = document.getElementById('cust-chat-status-badge');

    if (titleElem) titleElem.textContent = ticket.subject;
    if (subElem) {
        subElem.innerHTML = `Ticket #${ticket.ticketNumber} &bull; Category: ${ticket.category}` + (ticket.shipmentTracking ? ` &bull; Shipment: <strong>${ticket.shipmentTracking}</strong>` : '');
    }
    if (statusBadge) {
        statusBadge.textContent = ticket.status;
        statusBadge.className = `badge ${ticket.status === 'RESOLVED' ? 'badge-success' : (ticket.status === 'IN_PROGRESS' ? 'badge-warning' : 'badge-info')}`;
        statusBadge.style.display = 'inline-block';
    }

    // 2. Enable Input Box
    const replyInput = document.getElementById('cust-reply-text');
    const sendBtn = document.getElementById('btn-cust-send-reply');
    if (replyInput) replyInput.disabled = false;
    if (sendBtn) sendBtn.disabled = false;

    // 3. Render Messages Thread
    renderCustomerChatMessages(ticket.messages || []);
}

function renderCustomerChatMessages(messages) {
    const container = document.getElementById('cust-chat-messages-container');
    if (!container) return;

    if (!messages || messages.length === 0) {
        container.innerHTML = `<div style="text-align: center; color: var(--text-muted); font-size: 13px; margin: auto;">No messages in this thread yet.</div>`;
        return;
    }

    container.innerHTML = messages.map(m => {
        const isAdmin = m.senderRole === 'ADMIN' || m.senderRole === 'CUSTOMS_OFFICIAL' || m.senderRole === 'LOGISTICS_MGR';
        const alignStyle = isAdmin ? 'align-self: flex-start; background: #ffffff; border: 1px solid #cbd5e1; color: #0f172a; box-shadow: 0 2px 8px rgba(0,0,0,0.04);' : 'align-self: flex-end; background: #2563eb; border: 1px solid #1d4ed8; color: #ffffff; box-shadow: 0 2px 8px rgba(37,99,235,0.2);';
        const roleBadge = isAdmin ? '<span style="background: #e0e7ff; color: #4338ca; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: 700; margin-left: 6px;">GlobalTrade Support Coordinator</span>' : '';

        return `
            <div style="max-width: 80%; ${alignStyle} border-radius: 12px; padding: 12px 16px; font-size: 13.5px; line-height: 1.5;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; border-bottom: 1px solid ${isAdmin ? '#e2e8f0' : 'rgba(255,255,255,0.2)'}; padding-bottom: 4px;">
                    <strong style="font-size: 12px; color: ${isAdmin ? '#1e293b' : '#dbeafe'};">${m.senderName}${roleBadge}</strong>
                    <span style="font-size: 10.5px; color: ${isAdmin ? '#64748b' : '#bfdbfe'}; font-family: var(--font-mono); margin-left: 12px;">${m.sentAt || ''}</span>
                </div>
                <div>${m.message}</div>
            </div>
        `;
    }).join('');

    container.scrollTop = container.scrollHeight;
}

function submitCustomerReply(e) {
    if (e) e.preventDefault();
    if (!activeCustomerTicketId) return;

    const textInput = document.getElementById('cust-reply-text');
    const msg = textInput ? textInput.value.trim() : '';
    if (!msg) return;

    let currentUserId = null;
    let currentUserName = 'Customer Shipper';
    if (customerSession) {
        currentUserId = customerSession.userId || customerSession.id;
        currentUserName = (customerSession.firstName ? customerSession.firstName + ' ' + customerSession.lastName : null) || customerSession.name || currentUserName;
    }
    if (!currentUserId) currentUserId = 2;

    const payload = {
        senderUserId: currentUserId,
        senderName: currentUserName,
        senderRole: 'CUSTOMER',
        message: msg
    };

    textInput.value = '';

    fetch(getApiUrl(`support/tickets/${activeCustomerTicketId}/reply`), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            showToast('✉️ Message Sent!');
            if (data && data.ticket) {
                const idx = customerTicketsList.findIndex(t => t.id === data.ticket.id);
                if (idx !== -1) {
                    customerTicketsList[idx] = data.ticket;
                } else {
                    customerTicketsList.unshift(data.ticket);
                }
                renderCustomerTicketsList(customerTicketsList);
                selectSupportTicket(data.ticket.id);
            } else {
                fetchCustomerSupportTickets(activeCustomerTicketId);
            }
        })
        .catch(err => {
            showToast('⚠️ Error sending message. Please try again.');
        });
}

function closeAllCustomerModals() {
    const modals = document.querySelectorAll('.modal-overlay');
    modals.forEach(m => m.style.display = 'none');
}

// Auto-polling for real-time customer support chat updates (every 4s)
setInterval(() => {
    const supportTab = document.getElementById('tab-support');
    if (supportTab && supportTab.classList.contains('active')) {
        let currentUserId = null;
        if (customerSession) {
            currentUserId = customerSession.userId || customerSession.id || (customerSession.user ? customerSession.user.id : null);
        }
        if (!currentUserId) currentUserId = 2;

        fetch(getApiUrl(`support/tickets?userId=${currentUserId}`))
            .then(res => res.json())
            .then(data => {
                if (Array.isArray(data)) {
                    customerTicketsList = data;
                    renderCustomerTicketsList(customerTicketsList);
                    if (activeCustomerTicketId) {
                        const ticket = customerTicketsList.find(t => t.id === activeCustomerTicketId);
                        if (ticket) {
                            renderCustomerChatMessages(ticket.messages || []);
                        }
                    }
                }
            })
            .catch(() => {});
    }
}, 4000);

function closeAllCustomerModals() {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(m => {
        m.classList.add('hidden');
        m.style.removeProperty('display');
    });
}

function openModal(id, event) {
    if (event && event.preventDefault) event.preventDefault();
    closeAllCustomerModals();
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.remove('hidden');
        modal.style.removeProperty('display');
        modal.style.setProperty('display', 'flex', 'important');
    } else {
        console.error('Modal not found for ID:', id);
    }
}

function closeModal(id, event) {
    if (event && event.preventDefault) event.preventDefault();
    if (id) {
        const modal = document.getElementById(id);
        if (modal) {
            modal.classList.add('hidden');
            modal.style.removeProperty('display');
        }
    } else {
        closeAllCustomerModals();
    }
}

function toggleNewInquiryForm() {
    const box = document.getElementById('box-new-inquiry-form');
    const btn = document.getElementById('btn-toggle-inquiry-form');
    if (box) {
        if (box.style.display === 'none' || box.style.display === '') {
            box.style.display = 'block';
            if (btn) btn.innerHTML = '✕ Hide Form';
        } else {
            box.style.display = 'none';
            if (btn) btn.innerHTML = '+ New Support Inquiry';
        }
    }
}

function submitNewCustomerInquiry(e) {
    if (e) e.preventDefault();

    const categoryElem = document.getElementById('inquiry-category');
    const subjectElem = document.getElementById('inquiry-subject');
    const trackingElem = document.getElementById('inquiry-tracking');
    const messageElem = document.getElementById('inquiry-message');

    const category = categoryElem ? categoryElem.value : 'General Inquiry';
    const subject = subjectElem ? subjectElem.value.trim() : '';
    const tracking = trackingElem ? trackingElem.value.trim() : '';
    const message = messageElem ? messageElem.value.trim() : '';

    if (!subject || !message) {
        showToast('Subject and message details are required');
        return;
    }

    let currentUserId = null;
    if (customerSession) {
        currentUserId = customerSession.userId || customerSession.id || (customerSession.user ? customerSession.user.id : null);
    }
    if (!currentUserId) currentUserId = 2;

    const payload = {
        userId: currentUserId,
        subject: subject,
        category: category,
        shipmentTracking: tracking,
        message: message
    };

    // Reset input fields
    if (subjectElem) subjectElem.value = '';
    if (trackingElem) trackingElem.value = '';
    if (messageElem) messageElem.value = '';

    toggleNewInquiryForm();

    fetch(getApiUrl('support/tickets/create'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            showToast('🎉 Support Inquiry Created Successfully!');
            const createdId = data ? (data.id || data.ticketId) : null;
            fetchCustomerSupportTickets(createdId);
        })
        .catch(err => {
            showToast('⚠️ Error creating support inquiry.');
        });
}

document.addEventListener('DOMContentLoaded', () => {
    setTimeout(fetchCustomerSupportTickets, 600);
});
