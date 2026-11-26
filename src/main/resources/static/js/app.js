(function () {
    "use strict";

    var SIDEBAR_KEY = "igraded.sidebar";
    var mobileQuery = window.matchMedia("(max-width: 899px)");

    var body = document.body;
    var toggle = document.getElementById("sidebar-toggle");
    var backdrop = document.getElementById("backdrop");
    var sidebar = document.getElementById("sidebar");

    /* ---------- Light / dark theme ---------- */
    /* The saved theme is applied by a tiny script in <head> (before paint); this wires up the switch. */

    var THEME_KEY = "igraded.theme";
    var root = document.documentElement;
    var themeBtn = document.getElementById("theme-toggle");

    function setTheme(theme, remember) {
        root.setAttribute("data-theme", theme);
        if (themeBtn) themeBtn.setAttribute("aria-checked", String(theme === "dark"));
        if (remember) {
            try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* ignore */ }
        }
    }

    if (themeBtn) {
        setTheme(root.getAttribute("data-theme") === "light" ? "light" : "dark", false);
        themeBtn.addEventListener("click", function () {
            setTheme(root.getAttribute("data-theme") === "light" ? "dark" : "light", true);
        });
    }

    /* ---------- Sidebar ---------- */

    function isClosed() {
        return body.classList.contains("sidebar-closed");
    }

    function setSidebar(open, remember) {
        body.classList.toggle("sidebar-closed", !open);
        if (toggle) toggle.setAttribute("aria-expanded", String(open));
        if (backdrop) backdrop.hidden = !(open && mobileQuery.matches);

        // Only remember the choice made on a wide screen. On phones it always starts closed.
        if (remember && !mobileQuery.matches) {
            try { localStorage.setItem(SIDEBAR_KEY, open ? "open" : "closed"); } catch (e) { /* ignore */ }
        }
    }

    function initialSidebarState() {
        if (mobileQuery.matches) return false;
        try { return localStorage.getItem(SIDEBAR_KEY) !== "closed"; } catch (e) { return true; }
    }

    if (sidebar && toggle) {
        setSidebar(initialSidebarState(), false);

        toggle.addEventListener("click", function () {
            setSidebar(isClosed(), true);
        });

        if (backdrop) {
            backdrop.addEventListener("click", function () { setSidebar(false, false); });
        }

        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape" && mobileQuery.matches && !isClosed()) {
                setSidebar(false, false);
                toggle.focus();
            }
        });

        // Crossing the phone/desktop breakpoint: pick the right default for the new size
        var onBreakpoint = function () { setSidebar(initialSidebarState(), false); };
        if (mobileQuery.addEventListener) mobileQuery.addEventListener("change", onBreakpoint);
        else if (mobileQuery.addListener) mobileQuery.addListener(onBreakpoint);

        // Tapping a link inside the phone drawer closes it
        sidebar.addEventListener("click", function (e) {
            if (mobileQuery.matches && e.target.closest("a")) setSidebar(false, false);
        });
    }

    /* ---------- To-do ---------- */
    /* Tasks live in the database. Ticking a box tells the server; adding and deleting are normal form posts. */

    var todoList = document.getElementById("todo-list");

    if (todoList) {
        var addBtn = document.getElementById("todo-add");
        var form = document.getElementById("todo-form");
        var input = document.getElementById("todo-input");
        var summary = document.getElementById("todo-summary");

        function updateSummary() {
            var items = todoList.querySelectorAll(".todo__item");
            var open = 0;
            for (var n = 0; n < items.length; n++) {
                if (!items[n].classList.contains("is-done")) open++;
            }
            if (items.length === 0) summary.textContent = "No tasks yet";
            else if (open === 0) summary.textContent = "All caught up";
            else summary.textContent = open + (open === 1 ? " task to do" : " tasks to do");
        }

        function csrfValue(name) {
            var el = document.querySelector("#todo-form input[name=_csrf]");
            return el ? el.value : "";
        }

        todoList.addEventListener("change", function (e) {
            var li = e.target.closest(".todo__item");
            if (!li || e.target.type !== "checkbox") return;
            var box = e.target;
            var wanted = box.checked;
            li.classList.toggle("is-done", wanted);
            updateSummary();

            var data = new URLSearchParams();
            data.set("done", String(wanted));
            fetch("/todos/" + encodeURIComponent(li.getAttribute("data-id")) + "/done", {
                method: "POST",
                headers: { "X-CSRF-TOKEN": csrfValue() },
                body: data
            }).then(function (res) {
                if (!res.ok) throw new Error("failed");
            }).catch(function () {
                // The server did not save it, so put the box back
                box.checked = !wanted;
                li.classList.toggle("is-done", !wanted);
                updateSummary();
                alert("Could not save that change. Please try again.");
            });
        });

        addBtn.addEventListener("click", function () {
            var show = form.hidden;
            form.hidden = !show;
            addBtn.setAttribute("aria-expanded", String(show));
            if (show) input.focus();
        });
    }

    /* ---------- Locker upload form ---------- */
    /* Catches files that are too big, or would not fit, before a long upload starts. The server checks again. */

    var uploadForm = document.getElementById("upload-form");

    if (uploadForm) {
        var fileInput = document.getElementById("files");
        var uploadBtn = document.getElementById("upload-btn");
        var maxFile = Number(uploadForm.getAttribute("data-max"));
        var remaining = Number(uploadForm.getAttribute("data-remaining"));

        function sizeLabel(bytes) {
            if (bytes < 1024) return bytes + " B";
            var units = ["KB", "MB", "GB"];
            var v = bytes, u = -1;
            do { v /= 1024; u++; } while (v >= 1024 && u < units.length - 1);
            return (v >= 100 || v === Math.round(v) ? Math.round(v) : v.toFixed(1)) + " " + units[u];
        }

        uploadForm.addEventListener("submit", function (e) {
            var chosen = fileInput.files;
            var total = 0;
            for (var i = 0; i < chosen.length; i++) {
                if (maxFile && chosen[i].size > maxFile) {
                    e.preventDefault();
                    alert(chosen[i].name + " is larger than " + sizeLabel(maxFile) + ".");
                    return;
                }
                total += chosen[i].size;
            }
            if (total > remaining) {
                e.preventDefault();
                alert("These files need " + sizeLabel(total) + " but you only have " + sizeLabel(remaining) + " left.");
                return;
            }
            uploadBtn.disabled = true;           // stops double clicks while uploading
            uploadBtn.textContent = "Uploading...";
        });

        // Coming back with the Back button should not leave the button stuck
        window.addEventListener("pageshow", function () {
            uploadBtn.disabled = false;
            uploadBtn.textContent = "Upload";
        });
    }

    /* ---------- Announcement banner ---------- */

    var track = document.getElementById("banner-track");
    if (!track) return;

    var slides = track.children;
    var dots = document.querySelectorAll("#banner-dots .dot");
    var prev = document.getElementById("banner-prev");
    var next = document.getElementById("banner-next");
    var pauseBtn = document.getElementById("banner-pause");
    var banner = document.getElementById("banner");
    var count = slides.length;
    var index = 0;
    var timer = null;
    var paused = false;
    var AUTOPLAY_MS = 6000;
    var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    function show(i) {
        index = (i + count) % count;
        track.style.transform = "translateX(" + (-100 * index) + "%)";
        for (var s = 0; s < count; s++) {
            slides[s].setAttribute("aria-hidden", String(s !== index));
        }
        for (var d = 0; d < dots.length; d++) {
            dots[d].setAttribute("aria-selected", String(d === index));
        }
    }

    function stop() {
        if (timer) { clearInterval(timer); timer = null; }
    }

    function start() {
        if (reduceMotion || paused || count < 2) return;
        stop();
        timer = setInterval(function () { show(index + 1); }, AUTOPLAY_MS);
    }

    function go(i) { show(i); start(); }

    if (prev) prev.addEventListener("click", function () { go(index - 1); });
    if (next) next.addEventListener("click", function () { go(index + 1); });
    for (var d = 0; d < dots.length; d++) {
        (function (n) { dots[n].addEventListener("click", function () { go(n); }); })(d);
    }

    // Pause / play button in the card header
    if (pauseBtn) {
        pauseBtn.addEventListener("click", function () {
            paused = !paused;
            pauseBtn.setAttribute("aria-pressed", String(paused));
            pauseBtn.setAttribute("aria-label", paused ? "Play slideshow" : "Pause slideshow");
            if (paused) stop(); else start();
        });
    }

    // Also pause while the pointer or keyboard focus is on the banner
    banner.addEventListener("mouseenter", stop);
    banner.addEventListener("mouseleave", start);
    banner.addEventListener("focusin", stop);
    banner.addEventListener("focusout", start);

    // Swipe on touch screens
    var startX = null;
    banner.addEventListener("touchstart", function (e) {
        startX = e.touches[0].clientX;
        stop();
    }, { passive: true });
    banner.addEventListener("touchend", function (e) {
        if (startX !== null) {
            var dx = e.changedTouches[0].clientX - startX;
            if (Math.abs(dx) > 40) show(index + (dx < 0 ? 1 : -1));
            startX = null;
        }
        start();
    }, { passive: true });

    document.addEventListener("visibilitychange", function () {
        if (document.hidden) stop(); else start();
    });

    show(0);
    start();
})();
