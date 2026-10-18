import { reactive } from 'vue'

export const publishDialog = reactive({ visible: false })

export function openPublish() {
  publishDialog.visible = true
}
