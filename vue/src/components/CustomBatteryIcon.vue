<template>
  <i class="custom-battery-icon">
    <svg class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em">
      <path d="M642.048 447.488l-257.024 0 0-128 257.024 0 0 128zM642.048 640l-257.024 0 0-128 257.024 0 0 128zM642.048 831.488l-257.024 0 0-128 257.024 0 0 128zM705.536 126.976q26.624 0 49.664 10.24t40.448 27.648 27.648 40.448 10.24 49.664l0 641.024q0 26.624-10.24 49.664t-27.648 40.448-40.448 27.648-49.664 10.24l-384 0q-26.624 0-49.664-10.24t-40.448-27.648-27.648-40.448-10.24-49.664l0-641.024q0-26.624 10.24-49.664t27.648-40.448 40.448-27.648 49.664-10.24l63.488 0q0-26.624 18.944-45.056t45.568-18.432l128 0q26.624 0 45.568 18.432t18.944 45.056l63.488 0zM705.536 254.976l-384 0 0 641.024 384 0 0-641.024z" fill="currentColor"></path>
    </svg>
  </i>
</template>

<script>
export default {
  name: 'CustomBatteryIcon'
}
</script>

<style scoped>
.custom-battery-icon {
  font-family: "iconfont" !important;
  font-size: 16px;
  font-style: normal;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  position: relative;
  margin-right: 5px;
}
.icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.125em;
  fill: currentColor;
  overflow: hidden;
}
</style> 