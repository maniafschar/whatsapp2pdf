import { api } from "./api";

window.api = api;

document.getElementById('chatFile').onchange = () => {
    document.getElementsByTagName('attributes')[0].style.display = 'none';
    api.analyse();
};
