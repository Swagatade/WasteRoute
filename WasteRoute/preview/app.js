/* ==========================================================================
   WesteRoute — Interactive Application Logic & Hardware-Accelerated Navigation
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  // Navigation Screens
  const screenRole = document.getElementById('screen-role-selection');
  const screenCitizen = document.getElementById('screen-citizen-login');
  const screenCitizenRegister = document.getElementById('screen-citizen-register');
  const screenWorker = document.getElementById('screen-worker-login');
  const screenForgotPassword = document.getElementById('screen-forgot-password');
  const screenCitizenDashboard = document.getElementById('screen-citizen-dashboard');

  // Trigger elements
  const cardCitizen = document.getElementById('card-citizen');
  const cardWorker = document.getElementById('card-worker');
  const btnBackCitizen = document.getElementById('btn-back-citizen');
  const btnBackCitizenRegister = document.getElementById('btn-back-citizen-register');
  const btnBackWorker = document.getElementById('btn-back-worker');
  const btnGotoCitizenRegister = document.getElementById('btn-goto-citizen-register');
  const btnGotoCitizenLogin = document.getElementById('btn-goto-citizen-login');

  // Theme Toggle
  const globalThemeToggle = document.getElementById('global-theme-toggle');
  const htmlRoot = document.documentElement;

  // Device Switcher
  const btnPhone = document.getElementById('btn-phone');
  const btnTablet = document.getElementById('btn-tablet');
  const deviceContainer = document.getElementById('device-container');

  // Password Toggles
  const toggleCitizenPwd = document.getElementById('toggle-citizen-pwd');
  const citizenPwdInput = document.getElementById('citizen-password');
  const toggleCitizenRegPwd = document.getElementById('toggle-citizen-reg-pwd');
  const citizenRegPwdInput = document.getElementById('citizen-reg-password');
  const toggleCitizenRegConfirmPwd = document.getElementById('toggle-citizen-reg-confirm-pwd');
  const citizenRegConfirmPwdInput = document.getElementById('citizen-reg-confirm-password');
  const toggleWorkerPwd = document.getElementById('toggle-worker-pwd');
  const workerPwdInput = document.getElementById('worker-password');

  const toggleForgotNewPwd = document.getElementById('toggle-forgot-new-pwd');
  const forgotNewPwdInput = document.getElementById('forgot-new-pwd');
  const toggleForgotConfirmPwd = document.getElementById('toggle-forgot-confirm-pwd');
  const forgotConfirmPwdInput = document.getElementById('forgot-confirm-pwd');

  // Forgot password elements
  const forgotCard = document.getElementById('forgot-password-card');
  const forgotRoleBadge = document.getElementById('forgot-role-badge');
  const forgotEmailInput = document.getElementById('forgot-email');
  const forgotEmailError = document.getElementById('forgot-email-error');
  const forgotDisplayEmail = document.getElementById('forgot-display-email');
  const forgotOtpError = document.getElementById('forgot-otp-error');
  const forgotPwdError = document.getElementById('forgot-pwd-error');
  const otpCountdown = document.getElementById('otp-countdown');
  const linkResendOtp = document.getElementById('link-resend-otp');
  const otpTimerText = document.getElementById('otp-timer-text');
  const otpDigits = [
    document.getElementById('otp-1'),
    document.getElementById('otp-2'),
    document.getElementById('otp-3'),
    document.getElementById('otp-4'),
    document.getElementById('otp-5'),
    document.getElementById('otp-6')
  ];

  let currentForgotRole = 'citizen';
  let currentForgotStep = 1;
  let countdownTimer = null;
  let remainingSeconds = 45;

  // Time in status bar
  const statusTime = document.getElementById('status-time');
  const updateTime = () => {
    const now = new Date();
    const hours = now.getHours().toString().padStart(2, '0');
    const minutes = now.getMinutes().toString().padStart(2, '0');
    if (statusTime) statusTime.textContent = `${hours}:${minutes}`;
  };
  updateTime();
  setInterval(updateTime, 30000);

  // ==========================================
  // Tactile Micro-Animation & Slow Ripple System
  // ==========================================
  const createRipple = (e, targetEl) => {
    if (!targetEl) return;
    const rect = targetEl.getBoundingClientRect();
    const ripple = document.createElement('span');
    ripple.className = 'ripple-wave';
    const size = Math.max(rect.width, rect.height, 56);
    ripple.style.width = ripple.style.height = `${size}px`;

    const clientX = e && (e.clientX || (e.touches && e.touches[0] && e.touches[0].clientX))
      ? (e.clientX || e.touches[0].clientX)
      : (rect.left + rect.width / 2);

    const clientY = e && (e.clientY || (e.touches && e.touches[0] && e.touches[0].clientY))
      ? (e.clientY || e.touches[0].clientY)
      : (rect.top + rect.height / 2);

    ripple.style.left = `${clientX - rect.left - size / 2}px`;
    ripple.style.top = `${clientY - rect.top - size / 2}px`;

    targetEl.appendChild(ripple);
    setTimeout(() => {
      if (ripple.parentNode) ripple.remove();
    }, 620);
  };

  const triggerTactileAction = (element, callback, e, delayMs = 280) => {
    if (!element) {
      if (callback) callback();
      return;
    }

    const isCard = element.classList.contains('role-card') || element.classList.contains('glass-card') || element.classList.contains('floating-report-card');
    const pressClass = isCard ? 'card-pressing' : 'btn-pressing';

    element.classList.add(pressClass);
    createRipple(e, element);

    setTimeout(() => {
      element.classList.remove(pressClass);
      if (callback) callback();
    }, delayMs);
  };

  // Helper for slow, interactive button loading states (Sign in, Create Account, etc.)
  const handleSlowButtonAction = (button, defaultHtml, loadingText, successText, onComplete) => {
    if (!button || button.disabled) return;
    button.disabled = true;
    button.classList.add('btn-pressing');

    setTimeout(() => {
      button.classList.remove('btn-pressing');
      button.innerHTML = `<span class="btn-spinner"></span><span>${loadingText}</span>`;
      button.style.opacity = '0.92';

      setTimeout(() => {
        button.innerHTML = `<span class="btn-success-badge"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> ${successText}</span>`;
        button.style.opacity = '1';

        setTimeout(() => {
          button.innerHTML = defaultHtml;
          button.disabled = false;
          if (onComplete) onComplete();
        }, 650);
      }, 950);
    }, 280);
  };

  // ==========================================
  // Guaranteed Hardware Navigation Controller
  // ==========================================
  let isNavigating = false;

  const showScreen = (targetScreen, direction = 'forward') => {
    if (!targetScreen || isNavigating) return;
    isNavigating = true;

    const allScreens = [screenRole, screenCitizen, screenCitizenRegister, screenWorker, screenForgotPassword, screenCitizenDashboard];
    const currentActive = document.querySelector('.screen.active');

    if (currentActive === targetScreen) {
      isNavigating = false;
      return;
    }

    if (currentActive) {
      currentActive.classList.remove('active');
      if (direction === 'forward') {
        currentActive.classList.add('exit-left');
      } else {
        currentActive.classList.add('exit-right');
      }
    }

    allScreens.forEach(s => {
      if (s && s !== targetScreen) {
        s.classList.remove('active');
      }
    });

    targetScreen.classList.remove('exit-left', 'exit-right');
    targetScreen.classList.add('active');

    // Reset scroll to top
    const scrollable = targetScreen.querySelector('.screen-scrollable, .dashboard-scrollable');
    if (scrollable) {
      scrollable.scrollTop = 0;
    }

    setTimeout(() => {
      allScreens.forEach(s => {
        if (s && !s.classList.contains('active')) {
          s.classList.remove('exit-left', 'exit-right');
        }
      });
      isNavigating = false;
    }, 360);
  };

  // Helper for slow tactile tap binding
  const bindTactileTap = (element, actionHandler, delayMs = 280) => {
    if (!element) return;
    let touchMoved = false;

    element.addEventListener('touchstart', (e) => {
      touchMoved = false;
      element.classList.add(element.classList.contains('role-card') ? 'card-pressing' : 'btn-pressing');
    }, { passive: true });

    element.addEventListener('touchmove', () => {
      touchMoved = true;
      element.classList.remove('card-pressing', 'btn-pressing');
    }, { passive: true });

    element.addEventListener('touchend', (e) => {
      element.classList.remove('card-pressing', 'btn-pressing');
      if (!touchMoved) {
        e.preventDefault();
        e.stopPropagation();
        triggerTactileAction(element, () => actionHandler(e), e, delayMs);
      }
    });

    element.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      triggerTactileAction(element, () => actionHandler(e), e, delayMs);
    });
  };

  // Global methods for inline handlers
  window.westeRouteGoBack = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('button, a, .back-btn')) : null;
    triggerTactileAction(target, () => {
      showScreen(screenRole, 'backward');
    }, e, 260);
  };

  window.westeRouteShowCitizen = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('.role-card, button, a')) : null;
    const currentActive = document.querySelector('.screen.active');
    const direction = (currentActive === screenCitizenRegister || currentActive === screenForgotPassword || currentActive === screenCitizenDashboard) ? 'backward' : 'forward';
    triggerTactileAction(target, () => {
      showScreen(screenCitizen, direction);
    }, e, 300);
  };

  window.westeRouteShowCitizenRegister = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('button, a')) : null;
    triggerTactileAction(target, () => {
      showScreen(screenCitizenRegister, 'forward');
    }, e, 260);
  };

  window.westeRouteShowWorker = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('.role-card, button, a')) : null;
    const currentActive = document.querySelector('.screen.active');
    const direction = currentActive === screenForgotPassword ? 'backward' : 'forward';
    triggerTactileAction(target, () => {
      showScreen(screenWorker, direction);
    }, e, 300);
  };

  window.westeRouteShowCitizenDashboard = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    showScreen(screenCitizenDashboard, 'forward');
  };

  // ==========================================
  // Citizen Dashboard Interactions
  // ==========================================
  window.westeRouteSelectNavTab = (tabName) => {
    const tabs = ['home', 'complaints', 'map', 'profile'];
    tabs.forEach(t => {
      const tabEl = document.getElementById(`tab-${t}`);
      if (tabEl) {
        tabEl.classList.toggle('active', t === tabName);
      }
    });

    if (tabName === 'map') {
      const mapSec = document.getElementById('section-map-preview');
      if (mapSec) mapSec.scrollIntoView({ behavior: 'smooth' });
    }
  };

  window.westeRouteTriggerReport = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const reportCard = document.getElementById('btn-floating-report');
    triggerTactileAction(reportCard, () => {
      alert('📷 Camera & GPS Waste Reporter Activated!\n\nCitizens submit a garbage photo with automatic geolocation for YOLOv8 neural verification.');
    }, e, 250);
  };

  window.westeRouteToggleAiTooltip = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const tooltip = document.getElementById('ai-tooltip-content');
    if (tooltip) {
      tooltip.classList.toggle('hidden');
    }
  };

  window.westeRouteSelectBin = (name, dist, status, color) => {
    const popover = document.getElementById('bin-detail-popover');
    const nameEl = document.getElementById('popover-name');
    const subEl = document.getElementById('popover-sub');
    const dotEl = document.getElementById('popover-dot');

    if (nameEl) nameEl.textContent = name;
    if (subEl) subEl.textContent = `${dist} away · ${status}`;
    if (dotEl) dotEl.style.backgroundColor = color;
    if (popover) {
      popover.style.animation = 'none';
      popover.offsetHeight; // trigger reflow
      popover.style.animation = 'popIn 0.3s ease';
    }
  };

  window.westeRouteOpenQrScanner = () => {
    alert('▦ Household Doorstep Collection QR Scanner\n\nScan your municipal sanitation worker\'s badge QR code to confirm daily service.');
  };

  window.westeRouteShowComplaintsModal = () => {
    window.westeRouteSelectNavTab('complaints');
  };

  window.westeRouteFocusMap = () => {
    window.westeRouteSelectNavTab('map');
  };

  // ==========================================
  // Forgot Password Flow Controller
  // ==========================================
  window.westeRouteShowForgotPassword = (role = 'citizen', e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    currentForgotRole = role;
    const target = e ? (e.currentTarget || e.target.closest('a, button')) : null;

    triggerTactileAction(target, () => {
      if (forgotCard) {
        if (role === 'worker') {
          forgotCard.classList.add('worker-mode');
          if (forgotRoleBadge) {
            forgotRoleBadge.textContent = 'Worker Password Recovery';
            forgotRoleBadge.className = 'role-badge worker-badge';
          }
        } else {
          forgotCard.classList.remove('worker-mode');
          if (forgotRoleBadge) {
            forgotRoleBadge.textContent = 'Citizen Password Recovery';
            forgotRoleBadge.className = 'role-badge citizen-badge';
          }
        }
      }

      const buttons = [
        document.getElementById('btn-forgot-send-code'),
        document.getElementById('btn-forgot-verify-code'),
        document.getElementById('btn-forgot-reset-pwd'),
        document.getElementById('btn-forgot-success-signin')
      ];
      buttons.forEach(btn => {
        if (btn) {
          btn.className = role === 'worker' ? 'primary-button worker-btn' : 'primary-button citizen-btn';
        }
      });

      window.westeRouteForgotSetStep(1);
      showScreen(screenForgotPassword, 'forward');
    }, e, 260);
  };

  window.westeRouteForgotGoBack = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('button, a, .back-btn')) : null;
    triggerTactileAction(target, () => {
      if (currentForgotStep === 2) {
        window.westeRouteForgotSetStep(1);
      } else if (currentForgotStep === 3) {
        window.westeRouteForgotSetStep(2);
      } else {
        if (currentForgotRole === 'worker') {
          showScreen(screenWorker, 'backward');
        } else {
          showScreen(screenCitizen, 'backward');
        }
      }
    }, e, 260);
  };

  window.westeRouteForgotSetStep = (step, e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    currentForgotStep = step;

    for (let i = 1; i <= 4; i++) {
      const el = document.getElementById(`recovery-step-${i}`);
      if (el) {
        el.classList.toggle('active', i === step);
      }
    }

    const stepper = document.getElementById('recovery-stepper');
    if (stepper) {
      stepper.style.display = step === 4 ? 'none' : 'flex';
      for (let i = 1; i <= 3; i++) {
        const bar = document.getElementById(`step-bar-${i}`);
        if (bar) {
          bar.classList.toggle('active', i <= step);
        }
      }
    }
  };

  const startOtpTimer = () => {
    if (countdownTimer) clearInterval(countdownTimer);
    remainingSeconds = 45;
    if (otpTimerText) otpTimerText.style.display = 'inline';
    if (linkResendOtp) linkResendOtp.classList.add('hidden');
    if (otpCountdown) otpCountdown.textContent = `0:${remainingSeconds.toString().padStart(2, '0')}`;

    countdownTimer = setInterval(() => {
      remainingSeconds -= 1;
      if (remainingSeconds > 0) {
        if (otpCountdown) otpCountdown.textContent = `0:${remainingSeconds.toString().padStart(2, '0')}`;
      } else {
        clearInterval(countdownTimer);
        if (otpTimerText) otpTimerText.style.display = 'none';
        if (linkResendOtp) linkResendOtp.classList.remove('hidden');
      }
    }, 1000);
  };

  window.westeRouteResendOtp = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const target = e ? (e.currentTarget || e.target.closest('a, button')) : null;
    triggerTactileAction(target, () => {
      otpDigits.forEach(d => { if (d) d.value = ''; });
      if (otpDigits[0]) otpDigits[0].focus();
      if (forgotOtpError) forgotOtpError.textContent = '';
      startOtpTimer();
    }, e, 240);
  };

  window.westeRouteHandleForgotStep1 = (e) => {
    if (e) e.preventDefault();
    const btn = document.getElementById('btn-forgot-send-code');
    const emailVal = forgotEmailInput ? forgotEmailInput.value.trim() : '';
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(emailVal)) {
      if (forgotEmailError) forgotEmailError.textContent = 'Please enter a valid email address (e.g. name@example.com)';
      return;
    }
    if (forgotEmailError) forgotEmailError.textContent = '';

    handleSlowButtonAction(btn, '<span>Send Verification Code</span>', 'Sending code...', 'Code Sent!', () => {
      if (forgotDisplayEmail) forgotDisplayEmail.textContent = emailVal;
      window.westeRouteForgotSetStep(2);
      startOtpTimer();
      if (otpDigits[0]) setTimeout(() => otpDigits[0].focus(), 150);
    });
  };

  window.westeRouteHandleForgotStep2 = (e) => {
    if (e) e.preventDefault();
    const btn = document.getElementById('btn-forgot-verify-code');
    const code = otpDigits.map(d => d ? d.value : '').join('');
    if (code.length < 6) {
      if (forgotOtpError) forgotOtpError.textContent = 'Please enter the full 6-digit code';
      return;
    }
    if (forgotOtpError) forgotOtpError.textContent = '';

    handleSlowButtonAction(btn, '<span>Verify Code</span>', 'Verifying code...', 'Verified!', () => {
      window.westeRouteForgotSetStep(3);
    });
  };

  window.westeRouteCheckPwdStrength = (val) => {
    const strengthContainer = document.getElementById('forgot-strength-container');
    const strengthLabel = document.getElementById('forgot-strength-label');
    const strengthFill = document.getElementById('forgot-strength-fill');

    if (!strengthContainer || !strengthLabel || !strengthFill) return;

    if (!val || val.length === 0) {
      strengthContainer.classList.add('hidden');
      return;
    }
    strengthContainer.classList.remove('hidden');

    const hasMinLength = val.length >= 8;
    const hasNumber = /\d/.test(val);
    const hasUpperOrSpecial = /[A-Z]/.test(val) || /[^A-Za-z0-9]/.test(val);
    const score = (hasMinLength ? 1 : 0) + (hasNumber ? 1 : 0) + (hasUpperOrSpecial ? 1 : 0);

    if (score <= 1) {
      strengthLabel.textContent = 'Weak';
      strengthLabel.style.color = '#ef4444';
      strengthFill.style.width = '33%';
      strengthFill.style.backgroundColor = '#ef4444';
    } else if (score === 2) {
      strengthLabel.textContent = 'Moderate';
      strengthLabel.style.color = '#f59e0b';
      strengthFill.style.width = '66%';
      strengthFill.style.backgroundColor = '#f59e0b';
    } else {
      strengthLabel.textContent = 'Strong';
      strengthLabel.style.color = '#10b981';
      strengthFill.style.width = '100%';
      strengthFill.style.backgroundColor = '#10b981';
    }
  };

  window.westeRouteHandleForgotStep3 = (e) => {
    if (e) e.preventDefault();
    const btn = document.getElementById('btn-forgot-reset-pwd');
    const newPwd = forgotNewPwdInput ? forgotNewPwdInput.value : '';
    const confirmPwd = forgotConfirmPwdInput ? forgotConfirmPwdInput.value : '';

    if (newPwd.length < 6) {
      if (forgotPwdError) forgotPwdError.textContent = 'Password must be at least 6 characters';
      return;
    }
    if (newPwd !== confirmPwd) {
      if (forgotPwdError) forgotPwdError.textContent = 'Passwords do not match';
      return;
    }
    if (forgotPwdError) forgotPwdError.textContent = '';

    handleSlowButtonAction(btn, '<span>Reset Password</span>', 'Resetting password...', 'Password Reset!', () => {
      window.westeRouteForgotSetStep(4);
    });
  };

  // OTP Digit Auto-Advance setup
  otpDigits.forEach((digitInput, idx) => {
    if (!digitInput) return;

    digitInput.addEventListener('input', (e) => {
      const val = e.target.value;
      if (val.length > 0) {
        if (idx < otpDigits.length - 1 && otpDigits[idx + 1]) {
          otpDigits[idx + 1].focus();
        }
      }
    });

    digitInput.addEventListener('keydown', (e) => {
      if (e.key === 'Backspace' && digitInput.value === '' && idx > 0 && otpDigits[idx - 1]) {
        otpDigits[idx - 1].focus();
      }
    });

    digitInput.addEventListener('paste', (e) => {
      e.preventDefault();
      const pasteData = (e.clipboardData || window.clipboardData).getData('text').trim();
      if (/^\d+$/.test(pasteData)) {
        pasteData.slice(0, 6).split('').forEach((char, i) => {
          if (otpDigits[i]) otpDigits[i].value = char;
        });
        const focusIdx = Math.min(pasteData.length, 5);
        if (otpDigits[focusIdx]) otpDigits[focusIdx].focus();
      }
    });
  });

  // Bind role cards, navigation links & back buttons with slow tactile feedback
  bindTactileTap(cardCitizen, window.westeRouteShowCitizen, 300);
  bindTactileTap(cardWorker, window.westeRouteShowWorker, 300);
  bindTactileTap(btnBackCitizen, window.westeRouteGoBack, 260);
  bindTactileTap(btnBackCitizenRegister, window.westeRouteShowCitizen, 260);
  bindTactileTap(btnBackWorker, window.westeRouteGoBack, 260);
  bindTactileTap(btnGotoCitizenRegister, window.westeRouteShowCitizenRegister, 260);
  bindTactileTap(btnGotoCitizenLogin, window.westeRouteShowCitizen, 260);

  // Submit buttons tactile binding with automatic navigation to Citizen Dashboard!
  const submitCitizen = document.getElementById('btn-submit-citizen');
  const submitCitizenReg = document.getElementById('btn-submit-citizen-reg');
  const submitWorker = document.getElementById('btn-submit-worker');
  const submitForgotSuccess = document.getElementById('btn-forgot-success-signin');
  const btnGoogleCitizen = document.getElementById('btn-google-citizen');
  const btnGoogleCitizenReg = document.getElementById('btn-google-citizen-reg');
  const btnDemoAutofillCitizen = document.getElementById('btn-demo-autofill-citizen');
  const citizenAuthError = document.getElementById('citizen-auth-error');
  const citizenErrorText = document.getElementById('citizen-error-text');
  const googleAccountModal = document.getElementById('google-account-modal');
  const btnCloseGoogleModal = document.getElementById('btn-close-google-modal');
  const googleSigningInIndicator = document.getElementById('google-signing-in-indicator');
  const googleSigningInText = document.getElementById('google-signing-in-text');

  // Quick Demo Autofill
  if (btnDemoAutofillCitizen) {
    btnDemoAutofillCitizen.addEventListener('click', () => {
      const emailInput = document.getElementById('citizen-identifier');
      const pwdInput = document.getElementById('citizen-password');
      if (emailInput) emailInput.value = 'swagata@gmail.com';
      if (pwdInput) pwdInput.value = 'CitizenPass#2026';
      if (citizenAuthError) citizenAuthError.style.display = 'none';
      const wrapperEmail = document.getElementById('wrapper-citizen-email');
      const wrapperPwd = document.getElementById('wrapper-citizen-pwd');
      if (wrapperEmail) wrapperEmail.classList.remove('has-error');
      if (wrapperPwd) wrapperPwd.classList.remove('has-error');
    });
  }

  // Google Modal Open & Close
  function openGoogleModal() {
    if (googleAccountModal) {
      googleAccountModal.style.display = 'flex';
      if (googleSigningInIndicator) googleSigningInIndicator.style.display = 'none';
    }
  }

  function closeGoogleModal() {
    if (googleAccountModal) {
      googleAccountModal.style.display = 'none';
    }
  }

  if (btnCloseGoogleModal) {
    btnCloseGoogleModal.addEventListener('click', closeGoogleModal);
  }

  if (googleAccountModal) {
    googleAccountModal.addEventListener('click', (e) => {
      if (e.target === googleAccountModal) closeGoogleModal();
    });
  }

  // Handle Account Selection in Google Modal
  const googleAccountRows = document.querySelectorAll('.google-account-row:not(#row-add-google-account)');
  googleAccountRows.forEach((row) => {
    row.addEventListener('click', () => {
      const name = row.getAttribute('data-name') || 'Citizen';
      const email = row.getAttribute('data-email') || 'user@gmail.com';

      if (googleSigningInIndicator) {
        googleSigningInIndicator.style.display = 'flex';
        if (googleSigningInText) {
          googleSigningInText.textContent = `Signing in as ${name}...`;
        }
      }

      setTimeout(() => {
        closeGoogleModal();
        // Update user name in header
        const headerName = document.querySelector('.citizen-greeting strong');
        if (headerName) headerName.textContent = name;
        showScreen(screenCitizenDashboard, 'forward');
      }, 850);
    });
  });

  const rowAddGoogle = document.getElementById('row-add-google-account');
  if (rowAddGoogle) {
    rowAddGoogle.addEventListener('click', () => {
      const customEmail = prompt('Enter your Google email address:', 'swagata@gmail.com');
      if (customEmail && customEmail.includes('@')) {
        const name = customEmail.split('@')[0];
        if (googleSigningInIndicator) {
          googleSigningInIndicator.style.display = 'flex';
          if (googleSigningInText) googleSigningInText.textContent = `Signing in as ${name}...`;
        }
        setTimeout(() => {
          closeGoogleModal();
          const headerName = document.querySelector('.citizen-greeting strong');
          if (headerName) headerName.textContent = name;
          showScreen(screenCitizenDashboard, 'forward');
        }, 850);
      }
    });
  }

  // Google Authentication Button triggers modal
  if (btnGoogleCitizen) {
    btnGoogleCitizen.addEventListener('click', (e) => {
      e.preventDefault();
      openGoogleModal();
    });
  }

  if (btnGoogleCitizenReg) {
    btnGoogleCitizenReg.addEventListener('click', (e) => {
      e.preventDefault();
      openGoogleModal();
    });
  }

  // Email & Password Sign In with Validation
  if (submitCitizen) {
    submitCitizen.addEventListener('click', (e) => {
      e.preventDefault();
      const emailInput = document.getElementById('citizen-identifier');
      const pwdInput = document.getElementById('citizen-password');
      const emailVal = emailInput ? emailInput.value.trim() : '';
      const pwdVal = pwdInput ? pwdInput.value : '';
      const wrapperEmail = document.getElementById('wrapper-citizen-email');
      const wrapperPwd = document.getElementById('wrapper-citizen-pwd');

      // Validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      let hasError = false;

      if (!emailVal || !emailRegex.test(emailVal)) {
        if (wrapperEmail) wrapperEmail.classList.add('has-error');
        if (citizenErrorText) citizenErrorText.textContent = 'Please enter a valid email address.';
        if (citizenAuthError) citizenAuthError.style.display = 'flex';
        hasError = true;
      } else {
        if (wrapperEmail) wrapperEmail.classList.remove('has-error');
      }

      if (!pwdVal || pwdVal.length < 6) {
        if (wrapperPwd) wrapperPwd.classList.add('has-error');
        if (!hasError && citizenErrorText) citizenErrorText.textContent = 'Password must be at least 6 characters.';
        if (citizenAuthError) citizenAuthError.style.display = 'flex';
        hasError = true;
      } else {
        if (wrapperPwd) wrapperPwd.classList.remove('has-error');
      }

      if (hasError) return;

      if (citizenAuthError) citizenAuthError.style.display = 'none';

      handleSlowButtonAction(
        submitCitizen,
        '<span>Sign in with Email</span>',
        'Verifying credentials...',
        'Welcome Back!',
        () => {
          const name = emailVal.split('@')[0];
          const headerName = document.querySelector('.citizen-greeting strong');
          if (headerName) headerName.textContent = name.charAt(0).toUpperCase() + name.slice(1);
          showScreen(screenCitizenDashboard, 'forward');
        }
      );
    });
  }

  if (submitCitizenReg) {
    submitCitizenReg.addEventListener('click', (e) => {
      e.preventDefault();
      handleSlowButtonAction(
        submitCitizenReg,
        '<span>Create Account</span>',
        'Creating Account...',
        'Account Created!',
        () => {
          showScreen(screenCitizenDashboard, 'forward');
        }
      );
    });
  }

  const screenWorkerDashboard = document.getElementById('screen-worker-dashboard');

  if (submitWorker) {
    submitWorker.addEventListener('click', (e) => {
      e.preventDefault();
      handleSlowButtonAction(
        submitWorker,
        '<span>Sign in</span>',
        'Authenticating...',
        'Access Granted!',
        () => {
          showScreen(screenWorkerDashboard, 'forward');
          window.westeRouteInitWorkerData();
        }
      );
    });
  }

  if (submitForgotSuccess) {
    bindTactileTap(submitForgotSuccess, window.westeRouteForgotGoBack, 280);
  }

  // ==========================================
  // Theme Switching with Persistence
  // ==========================================
  const savedTheme = localStorage.getItem('westeroute_theme') || 'dark';
  htmlRoot.setAttribute('data-theme', savedTheme);

  const toggleTheme = (e) => {
    const target = globalThemeToggle;
    triggerTactileAction(target, () => {
      const currentTheme = htmlRoot.getAttribute('data-theme') || 'dark';
      const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
      htmlRoot.setAttribute('data-theme', nextTheme);
      try {
        localStorage.setItem('westeroute_theme', nextTheme);
      } catch (err) {}
    }, e, 200);
  };

  if (globalThemeToggle) {
    globalThemeToggle.addEventListener('click', toggleTheme);
  }

  // ==========================================
  // Device Frame Switcher
  // ==========================================
  if (btnPhone) {
    btnPhone.addEventListener('click', (e) => {
      triggerTactileAction(btnPhone, () => {
        btnPhone.classList.add('active');
        if (btnTablet) btnTablet.classList.remove('active');
        if (deviceContainer) deviceContainer.classList.remove('tablet');
      }, e, 200);
    });
  }

  if (btnTablet) {
    btnTablet.addEventListener('click', (e) => {
      triggerTactileAction(btnTablet, () => {
        btnTablet.classList.add('active');
        if (btnPhone) btnPhone.classList.remove('active');
        if (deviceContainer) deviceContainer.classList.add('tablet');
      }, e, 200);
    });
  }

  // ==========================================
  // Password Visibility Toggles
  // ==========================================
  const setupPasswordToggle = (toggleBtn, inputEl) => {
    if (!toggleBtn || !inputEl) return;
    toggleBtn.addEventListener('click', (e) => {
      triggerTactileAction(toggleBtn, () => {
        const isVisible = inputEl.type === 'text';
        inputEl.type = isVisible ? 'password' : 'text';
        toggleBtn.classList.toggle('visible', !isVisible);
      }, e, 150);
    });
  };

  setupPasswordToggle(toggleCitizenPwd, citizenPwdInput);
  setupPasswordToggle(toggleCitizenRegPwd, citizenRegPwdInput);
  setupPasswordToggle(toggleCitizenRegConfirmPwd, citizenRegConfirmPwdInput);
  setupPasswordToggle(toggleWorkerPwd, workerPwdInput);
  setupPasswordToggle(toggleForgotNewPwd, forgotNewPwdInput);
  setupPasswordToggle(toggleForgotConfirmPwd, forgotConfirmPwdInput);

  // =========================================================================
  // WORKER DASHBOARD STATE & INTERACTIVE CONTROLLERS
  // =========================================================================
  let isWorkerOnDuty = true;
  let isProofBeforeAdded = false;
  let isProofAfterAdded = false;
  let isAiVerified = false;
  let currentTaskStatus = 'IN_PROGRESS';
  let completedTasksCount = 3;
  let remainingTasksCount = 4;

  const mockWorkerTasks = [
    {
      id: '#WR-2048',
      title: 'Roadside Garbage',
      location: 'SRMIST Main Gate, Kattankulathur',
      distance: '450 m',
      priority: 'HIGH',
      status: 'IN_PROGRESS',
      assignedTime: '08:15 AM'
    },
    {
      id: '#WR-2050',
      title: 'Overflowing Public Bin',
      location: 'SRM Tech Park, Kattankulathur',
      distance: '850 m',
      priority: 'MEDIUM',
      status: 'ASSIGNED',
      assignedTime: '08:30 AM'
    },
    {
      id: '#WR-2053',
      title: 'Plastic Litter on Footpath',
      location: 'SRM Hostel Zone, Kattankulathur',
      distance: '1.1 km',
      priority: 'LOW',
      status: 'ASSIGNED',
      assignedTime: '09:00 AM'
    },
    {
      id: '#WR-2055',
      title: 'Illegal Dumping by Roadside',
      location: 'Potheri Main Road, Potheri',
      distance: '1.5 km',
      priority: 'URGENT',
      status: 'ASSIGNED',
      assignedTime: '09:45 AM'
    },
    {
      id: '#WR-2057',
      title: 'Green Waste & Dry Leaves Pile',
      location: 'SRM Hospital Area, Kattankulathur',
      distance: '1.2 km',
      priority: 'LOW',
      status: 'COMPLETED',
      assignedTime: '07:45 AM'
    }
  ];

  window.westeRouteInitWorkerData = () => {
    window.westeRouteFilterTasks('all');
  };

  window.westeRouteToggleDuty = () => {
    isWorkerOnDuty = !isWorkerOnDuty;
    const btnDuty = document.getElementById('btn-duty-toggle');
    const txtDuty = document.getElementById('duty-text');
    const dutyLock = document.getElementById('task-duty-lock');

    if (isWorkerOnDuty) {
      if (btnDuty) {
        btnDuty.className = 'duty-toggle-pill on-duty';
      }
      if (txtDuty) txtDuty.textContent = '● ON DUTY';
      if (dutyLock) dutyLock.classList.add('hidden');
    } else {
      if (btnDuty) {
        btnDuty.className = 'duty-toggle-pill off-duty';
      }
      if (txtDuty) txtDuty.textContent = '○ OFF DUTY';
      if (dutyLock) dutyLock.classList.remove('hidden');
    }
  };

  window.westeRouteEndShift = () => {
    if (confirm('Are you sure you want to end your shift for today? Your remaining tasks will be handed over to dispatch.')) {
      isWorkerOnDuty = false;
      window.westeRouteToggleDuty();
      alert('Shift ended. Thank you for keeping SRMIST campus clean!');
    }
  };

  window.westeRouteOpenStatusSheet = () => {
    if (!isWorkerOnDuty) {
      alert('You are currently OFF DUTY. Please switch to ON DUTY to update task status.');
      return;
    }
    const modal = document.getElementById('modal-task-status');
    if (modal) modal.classList.remove('hidden');
  };

  window.westeRouteCloseStatusSheet = () => {
    const modal = document.getElementById('modal-task-status');
    if (modal) modal.classList.add('hidden');
  };

  window.westeRouteSelectStatus = (status) => {
    currentTaskStatus = status;
    const statusChip = document.getElementById('current-task-status-chip');
    const stepCleaning = document.getElementById('step-row-cleaning');
    const stepComplete = document.getElementById('step-row-complete');

    if (statusChip) {
      statusChip.textContent = `● ${status.replace('_', ' ')}`;
      statusChip.className = `task-chip ${status === 'COMPLETED' ? 'chip-emerald' : 'chip-amber'}`;
    }

    if (status === 'COMPLETED') {
      completedTasksCount = 4;
      remainingTasksCount = 3;
      const compEl = document.getElementById('worker-stat-completed');
      const remEl = document.getElementById('worker-stat-remaining');
      const inProgEl = document.getElementById('worker-stat-inprogress');
      const progTasksDone = document.getElementById('prog-tasks-done');

      if (compEl) compEl.textContent = '04';
      if (remEl) remEl.textContent = '03';
      if (inProgEl) inProgEl.textContent = '00';
      if (progTasksDone) progTasksDone.textContent = '04';

      if (stepCleaning) stepCleaning.className = 'step-row completed';
      if (stepComplete) {
        stepComplete.className = 'step-row completed';
        stepComplete.innerHTML = '<div class="step-circle">✓</div><span class="text-emerald font-bold">Completed</span>';
      }
    }

    window.westeRouteCloseStatusSheet();
  };

  window.westeRouteToggleProofPhoto = (type) => {
    if (type === 'before') {
      isProofBeforeAdded = !isProofBeforeAdded;
      const box = document.getElementById('box-proof-before');
      const lbl = document.getElementById('lbl-proof-before');
      if (box && lbl) {
        box.classList.toggle('added', isProofBeforeAdded);
        lbl.textContent = isProofBeforeAdded ? '✓ Photo Added' : '+ Add Photo';
      }
    } else if (type === 'after') {
      isProofAfterAdded = !isProofAfterAdded;
      const box = document.getElementById('box-proof-after');
      const lbl = document.getElementById('lbl-proof-after');
      const checkingEl = document.getElementById('ai-proof-checking');
      const verifiedEl = document.getElementById('ai-proof-verified');

      if (box && lbl) {
        box.classList.toggle('added', isProofAfterAdded);
        lbl.textContent = isProofAfterAdded ? '✓ Photo Added' : '+ Add Photo';
      }

      if (isProofAfterAdded) {
        if (checkingEl) checkingEl.classList.remove('hidden');
        if (verifiedEl) verifiedEl.classList.add('hidden');
        isAiVerified = false;

        setTimeout(() => {
          if (checkingEl) checkingEl.classList.add('hidden');
          if (verifiedEl) verifiedEl.classList.remove('hidden');
          isAiVerified = true;
          window.westeRouteCheckProofCompletion();
        }, 1400);
      } else {
        if (checkingEl) checkingEl.classList.add('hidden');
        if (verifiedEl) verifiedEl.classList.add('hidden');
        isAiVerified = false;
      }
    }
    window.westeRouteCheckProofCompletion();
  };

  window.westeRouteCheckProofCompletion = () => {
    const btn = document.getElementById('btn-mark-task-completed');
    const msg = document.getElementById('proof-guide-msg');
    const canComplete = isProofBeforeAdded && isProofAfterAdded && isAiVerified;

    if (btn) btn.disabled = !canComplete;
    if (msg) {
      msg.textContent = canComplete
        ? '✓ Both photos verified. Ready to complete task.'
        : 'Add both before and after photos to verify and complete this task.';
    }
  };

  window.westeRouteCompleteTaskFromProof = () => {
    window.westeRouteSelectStatus('COMPLETED');
    alert('🎉 Task #WR-2048 marked as completed! Points awarded to Rahul Kumar.');
  };

  window.westeRouteNavigateGps = () => {
    alert('📍 Starting GPS Navigation to SRMIST Main Gate, Kattankulathur (450 m away). Turn left toward Grand Southern Trunk (GST) Road.');
  };

  window.westeRouteFilterTasks = (filter, btn) => {
    if (btn) {
      document.querySelectorAll('.task-filter-chips .filter-chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
    }

    const container = document.getElementById('assigned-task-container');
    const countEl = document.getElementById('task-filter-count');
    if (!container) return;

    let filtered = mockWorkerTasks;
    if (filter === 'urgent') filtered = mockWorkerTasks.filter(t => t.priority === 'URGENT');
    else if (filter === 'assigned') filtered = mockWorkerTasks.filter(t => t.status === 'ASSIGNED');
    else if (filter === 'in_progress') filtered = mockWorkerTasks.filter(t => t.status === 'IN_PROGRESS');
    else if (filter === 'completed') filtered = mockWorkerTasks.filter(t => t.status === 'COMPLETED');

    if (countEl) countEl.textContent = `${filtered.length} tasks`;

    container.innerHTML = filtered.map(task => {
      const prioColor = task.priority === 'URGENT' ? '#f43f5e' : (task.priority === 'HIGH' ? '#fb923c' : (task.priority === 'MEDIUM' ? '#f59e0b' : '#10b981'));
      const prioBg = task.priority === 'URGENT' ? 'rgba(244, 63, 94, 0.15)' : (task.priority === 'HIGH' ? 'rgba(251, 146, 60, 0.15)' : (task.priority === 'MEDIUM' ? 'rgba(245, 158, 11, 0.15)' : 'rgba(16, 185, 129, 0.15)'));

      return `
        <div class="assigned-task-item" onclick="alert('Viewing ${task.title} at ${task.location}')">
          <div class="task-item-left">
            <div class="task-prio-icon" style="background: ${prioBg}; color: ${prioColor};">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
            </div>
            <div class="task-item-details">
              <h5>${task.title} <span style="display:inline; font-weight: normal; color: var(--text-secondary);">${task.id}</span></h5>
              <span>📍 ${task.location}</span>
              <div class="task-item-meta">
                <span style="color: var(--cyan-400);">📏 ${task.distance}</span>
                <span style="color: ${prioColor}; background: ${prioBg}; padding: 1px 6px; border-radius: 4px;">${task.priority}</span>
                <span>Assigned ${task.assignedTime}</span>
              </div>
            </div>
          </div>
          <span style="color: var(--text-secondary); font-size: 16px;">→</span>
        </div>
      `;
    }).join('');
  };

  window.westeRouteOpenFullScreenQr = () => {
    const modal = document.getElementById('modal-worker-qr');
    if (modal) modal.classList.remove('hidden');
  };

  window.westeRouteCloseFullScreenQr = () => {
    const modal = document.getElementById('modal-worker-qr');
    if (modal) modal.classList.add('hidden');
  };

  let isHighBrightness = false;
  window.westeRouteToggleBrightness = () => {
    isHighBrightness = !isHighBrightness;
    const card = document.getElementById('full-qr-card-box');
    const btn = document.getElementById('btn-toggle-brightness');
    if (card) card.classList.toggle('high-brightness', isHighBrightness);
    if (btn) {
      btn.classList.toggle('active', isHighBrightness);
      btn.innerHTML = isHighBrightness
        ? '<span>✓ Max Brightness Active</span>'
        : '<span>Increase Screen Brightness</span>';
    }
  };

  window.westeRouteToggleOffline = (show) => {
    const banner = document.getElementById('worker-offline-banner');
    if (banner) {
      if (show) banner.classList.remove('hidden');
      else banner.classList.add('hidden');
    }
  };

  window.westeRouteSelectWorkerNav = (tab) => {
    document.querySelectorAll('.worker-bottom-nav .nav-tab').forEach(t => t.classList.remove('active'));
    const tabEl = document.getElementById(`worker-tab-${tab}`);
    if (tabEl) tabEl.classList.add('active');

    if (tab === 'home') {
      const scrollable = document.getElementById('worker-scrollable');
      if (scrollable) scrollable.scrollTo({ top: 0, behavior: 'smooth' });
    } else if (tab === 'tasks') {
      const sec = document.getElementById('assigned-task-container');
      if (sec) sec.scrollIntoView({ behavior: 'smooth' });
    } else if (tab === 'route') {
      const sec = document.querySelector('.route-timeline-card');
      if (sec) sec.scrollIntoView({ behavior: 'smooth' });
    } else if (tab === 'qr') {
      window.westeRouteOpenFullScreenQr();
    } else if (tab === 'profile') {
      alert('Worker Profile:\nRahul Kumar (WR-W-204)\nRole: Waste Collection Worker\nZone: SRM University Campus\nLocation: SRMIST, Kattankulathur\nVehicle Unit: EV-Tipper #TN-19-KTR-402');
    }
  };

  window.westeRouteFocusWorkerNotif = () => {
    const notifCard = document.querySelector('.worker-notif-card');
    if (notifCard) notifCard.scrollIntoView({ behavior: 'smooth' });
  };
});
