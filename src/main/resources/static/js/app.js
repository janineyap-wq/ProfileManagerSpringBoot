// js/app.js
// ================================================================
// The browser only ever talks to this server's own /api/profiles
// endpoints. avatar upload is now a single request: POST the file to
// /api/profiles/{id}/avatar, and this server compresses it to WebP,
// uploads it to Supabase Storage, and saves the URL to Postgres --
// all before responding. No separate origin, no CORS, no second
// "now save this URL" round trip.
// ================================================================

const API_BASE = "/api/profiles";
const DEFAULT_AVATAR =
  "https://6fkrqtkwbcnqsois.public.blob.vercel-storage.com/avatars/default.webp";

let currentProfileId = null;


// ================================================================
// Helpers
// ================================================================

function setStatus(message, isError = false) {
  const bar    = document.getElementById("status-message");
  const footer = document.getElementById("status-bar");
  bar.textContent         = message;
  footer.style.background = isError ? "#6b1a1a" : "var(--clr-status-bg)";
  footer.style.color      = isError ? "#ffcccc"  : "var(--clr-status-text)";
}

function clearCentrePanel() {
  document.getElementById("profile-pic").src           = DEFAULT_AVATAR;
  document.getElementById("profile-name").textContent   = "No Profile Selected";
  document.getElementById("profile-status").textContent = "\u2014";
  document.getElementById("profile-quote").textContent  = "\u2014";
  document.getElementById("friends-list").innerHTML     = "";
  currentProfileId = null;
}

// profile: { id, name, status, quote, picture, friends: [{id, name}, ...] }
function displayProfile(profile) {
  document.getElementById("profile-pic").src = profile.picture || DEFAULT_AVATAR;
  document.getElementById("profile-name").textContent   = profile.name;
  document.getElementById("profile-status").textContent = profile.status || "(no status set)";
  document.getElementById("profile-quote").textContent  = profile.quote  || "(no quote set)";
  currentProfileId = profile.id;
  renderFriendsList(profile.friends || []);
  setStatus(`Displaying ${profile.name}.`);
}

function renderFriendsList(friends) {
  const box = document.getElementById("friends-list");
  box.innerHTML = "";

  if (friends.length === 0) {
    box.innerHTML = '<p class="empty-state">No friends yet.</p>';
    return;
  }

  friends.forEach((f) => {
    const div = document.createElement("div");
    div.className   = "friend-entry";
    div.textContent = f.name;
    box.appendChild(div);
  });
}

function showUploadProgress(label = "Uploading...") {
  const wrapper = document.getElementById("upload-progress");
  const text    = document.getElementById("upload-progress-label");
  text.textContent = label;
  wrapper.hidden   = false;
}

function hideUploadProgress() {
  document.getElementById("upload-progress").hidden = true;
}

// api — small fetch wrapper. Reads the body as text first (a proxy/host
// error can return HTML instead of JSON) then parses it, and throws
// with the server's error message on non-2xx responses.
async function api(path, options = {}) {
  const response = await fetch(API_BASE + path, options);

  const rawText = await response.text();
  let body = null;
  if (rawText) {
    try {
      body = JSON.parse(rawText);
    } catch {
      throw new Error(`Server returned HTTP ${response.status} (not JSON).`);
    }
  }

  if (!response.ok) {
    throw new Error((body && body.error) || `Server error ${response.status}.`);
  }

  return body;
}

function apiJson(path, method, payload) {
  return api(path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}


// ================================================================
// CRUD Functions
// ================================================================

async function loadProfileList() {
  try {
    const profiles = await api("");

    const container = document.getElementById("profile-list");
    container.innerHTML = "";

    if (profiles.length === 0) {
      container.innerHTML = '<p class="empty-state">No profiles found.</p>';
      return;
    }

    profiles.forEach((profile) => {
      const row = document.createElement("div");
      row.className  = "profile-item";
      row.dataset.id = profile.id;

      const img = document.createElement("img");
      img.className = "list-thumb";
      img.src = profile.picture || DEFAULT_AVATAR;
      img.alt = profile.name;
      img.onerror = () => { img.src = DEFAULT_AVATAR; };

      const span = document.createElement("span");
      span.textContent = profile.name;

      row.appendChild(img);
      row.appendChild(span);
      row.addEventListener("click", () => selectProfile(profile.id));
      container.appendChild(row);
    });

  } catch (err) {
    setStatus(`Error loading profiles: ${err.message}`, true);
  }
}


async function selectProfile(profileId) {
  try {
    document.querySelectorAll("#profile-list .profile-item").forEach((el) => {
      el.classList.toggle("active", el.dataset.id === profileId);
    });

    const profile = await api(`/${profileId}`);
    displayProfile(profile);

  } catch (err) {
    setStatus(`Error selecting profile: ${err.message}`, true);
  }
}


async function addProfile() {
  const nameInput = document.getElementById("input-name");
  const name      = nameInput.value.trim();

  if (!name) {
    setStatus("Error: Name field is empty. Please enter a name.", true);
    return;
  }

  try {
    const created = await apiJson("", "POST", { name });

    nameInput.value = "";
    await loadProfileList();
    document.querySelectorAll("#profile-list .profile-item").forEach((el) => {
      el.classList.toggle("active", el.dataset.id === created.id);
    });
    displayProfile(created);
    setStatus(`Profile "${name}" created successfully.`);

  } catch (err) {
    setStatus(`Error adding profile: ${err.message}`, true);
  }
}


async function lookUpProfile() {
  const query = document.getElementById("input-name").value.trim();

  if (!query) {
    setStatus("Error: Name field is empty. Please enter a name to search.", true);
    return;
  }

  try {
    const profile = await api(`/lookup?query=${encodeURIComponent(query)}`);
    document.querySelectorAll("#profile-list .profile-item").forEach((el) => {
      el.classList.toggle("active", el.dataset.id === profile.id);
    });
    displayProfile(profile);

  } catch (err) {
    setStatus(err.message, true);
    clearCentrePanel();
  }
}


async function deleteProfile() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected. Click a profile in the list first.", true);
    return;
  }

  const name = document.getElementById("profile-name").textContent;

  if (!window.confirm(`Delete the profile for "${name}"? This cannot be undone.`)) {
    setStatus("Deletion cancelled.");
    return;
  }

  try {
    await api(`/${currentProfileId}`, { method: "DELETE" });

    clearCentrePanel();
    await loadProfileList();
    setStatus(`Profile "${name}" deleted. All friendship records removed automatically.`);

  } catch (err) {
    setStatus(`Error deleting profile: ${err.message}`, true);
  }
}


async function changeStatus() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected.", true);
    return;
  }
  const newStatus = document.getElementById("input-status").value.trim();
  if (!newStatus) {
    setStatus("Error: Status field is empty.", true);
    return;
  }
  try {
    await apiJson(`/${currentProfileId}/status`, "PATCH", { status: newStatus });

    document.getElementById("profile-status").textContent = newStatus;
    document.getElementById("input-status").value = "";
    setStatus("Status updated.");

  } catch (err) {
    setStatus(`Error updating status: ${err.message}`, true);
  }
}


async function changeQuote() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected.", true);
    return;
  }
  const newQuote = document.getElementById("input-quote").value.trim();
  if (!newQuote) {
    setStatus("Error: Quote field is empty.", true);
    return;
  }
  try {
    await apiJson(`/${currentProfileId}/quote`, "PATCH", { quote: newQuote });

    document.getElementById("profile-quote").textContent = newQuote;
    document.getElementById("input-quote").value = "";
    setStatus("Favorite quote updated.");

  } catch (err) {
    setStatus(`Error updating quote: ${err.message}`, true);
  }
}


// ================================================================
// Picture Update
// ================================================================

async function changePicture() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected.", true);
    return;
  }

  const fileInput = document.getElementById("input-picture-file");
  const urlInput  = document.getElementById("input-picture-url");
  const file      = fileInput.files[0];
  const urlValue  = urlInput.value.trim();

  if (file) {
    await uploadAvatarFile(file);
    return;
  }

  if (urlValue) {
    await savePictureUrl(urlValue);
    return;
  }

  setStatus("Error: Select a file or enter a URL before clicking Update Picture.", true);
}


// Mode A: upload straight to this server. It compresses to WebP,
// uploads to Supabase Storage, and persists the URL -- one request.
async function uploadAvatarFile(file) {
  if (!file.type.startsWith("image/")) {
    setStatus("Error: The selected file is not an image.", true);
    return;
  }

  showUploadProgress("Compressing and uploading...");
  setStatus("Uploading image...");

  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${API_BASE}/${currentProfileId}/avatar`, {
      method: "POST",
      body: formData,
      // No Content-Type header set manually -- the browser adds the
      // multipart boundary automatically when the body is FormData.
    });

    const rawText = await response.text();
    let result;
    try {
      result = JSON.parse(rawText);
    } catch {
      throw new Error(`Server returned HTTP ${response.status} (not JSON).`);
    }

    if (!response.ok) {
      throw new Error(result.error || `Server error ${response.status}.`);
    }

    applyNewPicture(result.url);
    document.getElementById("input-picture-file").value = "";
    setStatus("Picture updated successfully.");

  } catch (err) {
    setStatus("Error uploading image: " + err.message, true);
  } finally {
    hideUploadProgress();
  }
}


// Mode B: paste a URL directly.
async function savePictureUrl(url) {
  if (!url.startsWith("https://")) {
    setStatus("Error: URL must start with https://", true);
    return;
  }

  setStatus("Saving picture URL...");

  try {
    await apiJson(`/${currentProfileId}/picture`, "PATCH", { pictureUrl: url });
    applyNewPicture(url);
    document.getElementById("input-picture-url").value = "";
    setStatus("Picture updated successfully.");

  } catch (err) {
    setStatus(`Error saving URL: ${err.message}`, true);
  }
}


function applyNewPicture(url) {
  document.getElementById("profile-pic").src = url;

  const activeThumb = document.querySelector("#profile-list .profile-item.active .list-thumb");
  if (activeThumb) activeThumb.src = url;
}


// ================================================================
// Friends Management
// ================================================================

async function addFriend() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected.", true);
    return;
  }
  const friendName = document.getElementById("input-friend").value.trim();
  if (!friendName) {
    setStatus("Error: Friend name field is empty.", true);
    return;
  }

  try {
    const result = await apiJson(`/${currentProfileId}/friends`, "POST", { friendName });

    document.getElementById("input-friend").value = "";
    await selectProfile(currentProfileId);
    setStatus(`"${result.friendName}" added as a friend (bidirectional).`);

  } catch (err) {
    setStatus(`Error adding friend: ${err.message}`, true);
  }
}


async function removeFriend() {
  if (!currentProfileId) {
    setStatus("Error: No profile is selected.", true);
    return;
  }
  const friendName = document.getElementById("input-friend").value.trim();
  if (!friendName) {
    setStatus("Error: Friend name field is empty.", true);
    return;
  }

  try {
    const result = await apiJson(`/${currentProfileId}/friends`, "DELETE", { friendName });

    document.getElementById("input-friend").value = "";
    await selectProfile(currentProfileId);
    setStatus(`"${result.friendName}" removed from friends (both directions).`);

  } catch (err) {
    setStatus(`Error removing friend: ${err.message}`, true);
  }
}


// ================================================================
// Event Listener Setup
// ================================================================

document.addEventListener("DOMContentLoaded", async () => {

  document.getElementById("btn-add").addEventListener("click", addProfile);
  document.getElementById("btn-lookup").addEventListener("click", lookUpProfile);
  document.getElementById("btn-delete").addEventListener("click", deleteProfile);

  document.getElementById("input-name").addEventListener("keydown", (e) => {
    if (e.key === "Enter") addProfile();
  });

  document.getElementById("btn-status").addEventListener("click", changeStatus);
  document.getElementById("input-status").addEventListener("keydown", (e) => {
    if (e.key === "Enter") changeStatus();
  });

  document.getElementById("btn-quote").addEventListener("click", changeQuote);
  document.getElementById("input-quote").addEventListener("keydown", (e) => {
    if (e.key === "Enter") changeQuote();
  });

  document.getElementById("btn-picture").addEventListener("click", changePicture);

  document.getElementById("input-picture-file").addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) return;

    const pic = document.getElementById("profile-pic");
    if (pic.dataset.previewUrl) {
      URL.revokeObjectURL(pic.dataset.previewUrl);
    }

    const previewUrl = URL.createObjectURL(file);
    pic.src                = previewUrl;
    pic.dataset.previewUrl = previewUrl;
    setStatus("Preview loaded. Click 'Update Picture' to save.");
  });

  document.getElementById("btn-add-friend").addEventListener("click", addFriend);
  document.getElementById("btn-remove-friend").addEventListener("click", removeFriend);

  document.getElementById("btn-exit").addEventListener("click", () => {
    setStatus("To exit, close this browser tab.");
    if (!window.close()) {
      // Window couldn't close - user already has instructions
    }
  });

  await loadProfileList();
  setStatus("Ready. Select a profile from the list or add a new one.");
});
