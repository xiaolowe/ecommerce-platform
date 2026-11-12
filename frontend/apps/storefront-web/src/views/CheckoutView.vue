<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAddress,
  createOrder,
  listAddresses,
  type Address,
} from '../services/api'
import { apiErrorMessage } from '../services/http'
import { useCartStore } from '../stores/cart'
import { formatMoney } from '../utils/money'

const cart = useCartStore()
const router = useRouter()
const addresses = ref<Address[]>([])
const selectedAddressId = ref('')
const loading = ref(true)
const savingAddress = ref(false)
const placingOrder = ref(false)
const showAddressForm = ref(false)
const error = ref('')
const form = reactive({
  recipientName: '',
  recipientPhone: '',
  region: '',
  detail: '',
  defaultAddress: false,
})

const currency = computed(() => cart.lines[0]?.currency ?? 'CNY')

async function loadAddresses() {
  addresses.value = await listAddresses()
  const selected =
    addresses.value.find((address) => address.id === selectedAddressId.value) ??
    addresses.value.find((address) => address.defaultAddress) ??
    addresses.value[0]
  selectedAddressId.value = selected?.id ?? ''
  showAddressForm.value = addresses.value.length === 0
}

async function saveAddress() {
  if (
    !form.recipientName.trim() ||
    !form.recipientPhone.trim() ||
    !form.region.trim() ||
    !form.detail.trim()
  ) {
    error.value = '请填写完整的收货地址'
    return
  }
  savingAddress.value = true
  error.value = ''
  try {
    const address = await createAddress({
      recipientName: form.recipientName.trim(),
      recipientPhone: form.recipientPhone.trim(),
      region: form.region.trim(),
      detail: form.detail.trim(),
      defaultAddress: form.defaultAddress || addresses.value.length === 0,
    })
    selectedAddressId.value = address.id
    Object.assign(form, {
      recipientName: '',
      recipientPhone: '',
      region: '',
      detail: '',
      defaultAddress: false,
    })
    showAddressForm.value = false
    await loadAddresses()
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    savingAddress.value = false
  }
}

async function placeOrder() {
  if (!cart.lines.length) {
    error.value = '购物袋为空'
    return
  }
  if (!selectedAddressId.value) {
    error.value = '请选择收货地址'
    return
  }
  placingOrder.value = true
  error.value = ''
  try {
    const order = await createOrder(
      cart.lines.map((line) => ({ productId: line.productId, quantity: line.quantity })),
      selectedAddressId.value,
    )
    await cart.load()
    await router.push({ name: 'order', params: { id: order.id } })
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    placingOrder.value = false
  }
}

onMounted(async () => {
  try {
    await Promise.all([cart.load(), loadAddresses()])
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="checkout-page">
    <div class="section-heading">
      <div>
        <p class="kicker">CHECKOUT</p>
        <h1>确认订单</h1>
      </div>
      <span>{{ cart.count }} 件商品</span>
    </div>

    <p v-if="error || cart.error" class="notice">{{ error || cart.error }}</p>
    <p v-if="loading || cart.loading" class="notice neutral">正在准备结算信息…</p>

    <div v-else-if="cart.lines.length" class="checkout-grid">
      <div class="checkout-main">
        <section class="checkout-panel">
          <div class="checkout-panel-heading">
            <h2>收货地址</h2>
            <button class="text-button" @click="showAddressForm = !showAddressForm">
              {{ showAddressForm ? '收起' : '新增地址' }}
            </button>
          </div>

          <div v-if="addresses.length" class="address-list">
            <label
              v-for="address in addresses"
              :key="address.id"
              class="address-card"
              :class="{ selected: selectedAddressId === address.id }"
            >
              <input v-model="selectedAddressId" type="radio" :value="address.id" />
              <span>
                <strong>{{ address.recipientName }} · {{ address.recipientPhone }}</strong>
                <small>{{ address.region }} {{ address.detail }}</small>
              </span>
              <b v-if="address.defaultAddress">默认</b>
            </label>
          </div>

          <form v-if="showAddressForm" class="address-form" @submit.prevent="saveAddress">
            <label>
              收货人
              <input v-model="form.recipientName" autocomplete="name" />
            </label>
            <label>
              手机号
              <input v-model="form.recipientPhone" autocomplete="tel" />
            </label>
            <label>
              省市区
              <input v-model="form.region" placeholder="例如：上海市 浦东新区" />
            </label>
            <label class="address-detail">
              详细地址
              <input v-model="form.detail" autocomplete="street-address" />
            </label>
            <label class="default-check">
              <input v-model="form.defaultAddress" type="checkbox" />
              设为默认地址
            </label>
            <button class="secondary-button" type="submit" :disabled="savingAddress">
              {{ savingAddress ? '保存中…' : '保存地址' }}
            </button>
          </form>
        </section>

        <section class="checkout-panel">
          <h2>商品清单</h2>
          <div class="checkout-lines">
            <div v-for="line in cart.lines" :key="line.productId">
              <span>{{ line.name }} × {{ line.quantity }}</span>
              <strong>{{ formatMoney(line.priceCents * line.quantity, line.currency) }}</strong>
            </div>
          </div>
        </section>
      </div>

      <aside class="checkout-summary">
        <span>应付合计</span>
        <strong>{{ formatMoney(cart.subtotalCents, currency) }}</strong>
        <small>价格以提交订单时服务端校验结果为准</small>
        <button
          class="primary-button"
          :disabled="placingOrder || !selectedAddressId"
          @click="placeOrder"
        >
          {{ placingOrder ? '创建订单中…' : '提交订单' }}
        </button>
      </aside>
    </div>

    <div v-else class="empty-state">
      <h2>购物袋为空</h2>
      <RouterLink to="/" class="primary-button">继续购物</RouterLink>
    </div>
  </section>
</template>
