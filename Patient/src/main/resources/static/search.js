/**
 * 
 */
//document.getElementById("namae")
//    .addEventListener("input", function() {
//
//    const namae = this.value;
//
//    fetch("/search?namae=" + namae)
//        .then(response => response.text())
//        .then(data => {
//            document.getElementById("result").innerHTML = data;
//        });
//});

/**
 * ユーザ検索(インクリメンタルサーチ)
 * ・IME変換中(予測変換の候補が出ている間)は何もしない
 * ・変換確定した瞬間に検索を実行
 * ・変換を伴わない直接入力(1文字追加など)も都度検索
 * ・空文字に戻したときは初期の全件表示に戻す
 * ・テキストボックスの中身は消さない
 * ・検索中は「もっと見る」ボタンを隠し、初期表示に戻ったら復活させる
 */
(function () {
    const input = document.getElementById("namae");
    const patientList = document.getElementById("patientList");
    const moreButton = document.getElementById("moreButton");
    const form = input.closest("form");

    let isComposing = false;

    // Enterキーによるフォーム送信(ページ遷移)を止める
    form?.addEventListener("submit", function (e) {
        e.preventDefault();
    });

    input.addEventListener("compositionstart", function () {
        isComposing = true;
    });

    input.addEventListener("compositionend", function () {
        isComposing = false;
        handleInputChange();
    });

    input.addEventListener("input", function () {
        if (isComposing) {
            return;
        }
        handleInputChange();
    });

    function handleInputChange() {
        const namae = input.value.trim();

        if (namae === "") {
            resetToInitialList();
        } else {
            doSearch(namae);
        }
    }

    function doSearch(namae) {
        fetch("/detail?namae=" + encodeURIComponent(namae))
            .then(function (response) {
                return response.text();
            })
            .then(function (html) {
                replacePatientList(html);

                // 検索中は「もっと見る」ボタンを隠す
                if (moreButton) {
                    moreButton.style.display = "none";
                }
            });
    }

    function resetToInitialList() {
        // namaeパラメータなしでアクセスし、全件(updateDate降順)を取得
        fetch("/detail")
            .then(function (response) {
                return response.text();
            })
            .then(function (html) {
                replacePatientList(html);

                // ページングを最初からやり直せるようにoffsetをリセット
                window.offset = 20;

                // 「もっと見る」ボタンを復活させる
                if (moreButton) {
                    moreButton.style.display = "";
                }
            });
    }

    function replacePatientList(html) {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, "text/html");
        const newList = doc.getElementById("patientList");

        if (newList) {
            patientList.innerHTML = newList.innerHTML;
			document.querySelector(".new-message")?.remove();
        }
    }
})();