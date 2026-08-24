/**
 * 
 */
fetch("/common/header-menu.html")
.then(r => r.text())
.then(html=>{
    document.getElementById("header").innerHTML=html;

document.addEventListener("click", function(event) {

    document.querySelectorAll(".header-menu details").forEach(menu => {

        if (!menu.contains(event.target)) {
            menu.removeAttribute("open");
        }

    });

})});

function openLogoutDialog(event) {
	event.preventDefault();

	 // プルダウンメニューを閉じる
	 document.querySelectorAll(".header-menu details").forEach(menu => {
	     menu.removeAttribute("open");
	 });

	 // ログアウト確認ダイアログを表示
	 document.getElementById("logoutDialog").style.display = "flex";

}


function closeLogoutDialog() {

    document.getElementById("logoutDialog").style.display = "none";

}
