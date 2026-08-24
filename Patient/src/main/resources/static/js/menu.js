/**
 * 
 */
document.addEventListener("click", function(event) {

    document.querySelectorAll(".header-menu details").forEach(menu => {

        if (!menu.contains(event.target)) {
            menu.removeAttribute("open");
        }

    });

});