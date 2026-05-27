let currentActiveAnchorId = null;
let headingObserver = null;

function clearSearchHighlights() {
  document.querySelectorAll("mark.md-reader-hit").forEach(function (node) {
    const parent = node.parentNode;
    if (!parent) return;
    parent.replaceChild(document.createTextNode(node.textContent), node);
    parent.normalize();
  });
}

function jumpToAnchor(anchorId) {
  const node = document.getElementById(anchorId);
  if (node) {
    if (window.location.hash !== "#" + anchorId) {
      history.replaceState(null, "", "#" + anchorId);
    }
    node.scrollIntoView(true);
    reportActiveAnchor(anchorId);
    return true;
  }
  return false;
}

function reportActiveAnchor(anchorId) {
  if (!anchorId || currentActiveAnchorId === anchorId) {
    return;
  }

  currentActiveAnchorId = anchorId;
  document.querySelectorAll(".is-active-heading").forEach(function (heading) {
    heading.classList.remove("is-active-heading");
  });
  document.querySelectorAll(".toc-link.is-active").forEach(function (link) {
    link.classList.remove("is-active");
  });

  const activeNode = document.getElementById(anchorId);
  if (activeNode) {
    activeNode.classList.add("is-active-heading");
  }
  const activeLink = document.querySelector('.toc-link[data-anchor="' + anchorId + '"]');
  if (activeLink) {
    activeLink.classList.add("is-active");
  }
}

function installHeadingObserver() {
  if (headingObserver) {
    headingObserver.disconnect();
  }

  const headings = Array.from(document.querySelectorAll("h1[id], h2[id], h3[id], h4[id], h5[id], h6[id]"));
  if (!headings.length) {
    return;
  }

  headingObserver = new IntersectionObserver(
    function (entries) {
      const visible = entries
        .filter(function (entry) {
          return entry.isIntersecting;
        })
        .sort(function (a, b) {
          return a.boundingClientRect.top - b.boundingClientRect.top;
        });

      if (visible.length) {
        reportActiveAnchor(visible[0].target.id);
      }
    },
    {
      rootMargin: "0px 0px -70% 0px",
      threshold: [0, 0.2, 0.5, 1]
    }
  );

  headings.forEach(function (heading) {
    headingObserver.observe(heading);
  });

  reportActiveAnchor(headings[0].id);
}

window.addEventListener("load", function () {
  installHeadingObserver();
  if (window.location.hash) {
    const anchorId = window.location.hash.slice(1);
    if (anchorId) {
      jumpToAnchor(anchorId);
    }
  }
});

window.addEventListener("hashchange", function () {
  const anchorId = window.location.hash.slice(1);
  if (anchorId) {
    reportActiveAnchor(anchorId);
  }
});
