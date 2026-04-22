// Общие функции для всего приложения

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU');
}

function showError(message) {
    console.error(message);
    // Можно добавить toast-уведомления
}

function showSuccess(message) {
    console.log(message);
    // Можно добавить toast-уведомления
}