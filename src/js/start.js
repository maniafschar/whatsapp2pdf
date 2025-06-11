import { api } from "./api";

window.api = api;

document.getElementById('chatFile').onchange = () => {
    document.getElementsByTagName('attributes')[0].style.display = 'none';
    api.analyse();
};

function showDescription(i) {
    document.querySelector('description container').style.marginLeft = -(i * 100) + '%';
    document.querySelector('tab.selected')?.classList.remove('selected');
    document.querySelectorAll('tab')[i].classList.add('selected');
}

function feedback() {
}
