// Whether the pages animate. By default this follows the system setting ("reduce motion" / Windows
// "Animation effects"); the switch on the live page overrides it, remembered in this browser.
const KEY = 'pp-motion';
const systemWantsLess = window.matchMedia('(prefers-reduced-motion: reduce)');

function saved() {
  try {
    return localStorage.getItem(KEY);
  } catch {
    return null; // private mode or blocked storage: follow the system
  }
}

export function animationsOn() {
  const choice = saved();
  if (choice === 'on') return true;
  if (choice === 'off') return false;
  return !systemWantsLess.matches;
}

export function setAnimations(on) {
  try {
    localStorage.setItem(KEY, on ? 'on' : 'off');
  } catch {
    // the choice then lasts only until the page is closed
  }
  document.documentElement.dataset.motion = on ? 'on' : 'off';
  document.dispatchEvent(new CustomEvent('motion:change', { detail: { on } }));
}
