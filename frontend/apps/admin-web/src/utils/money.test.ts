import { describe, expect, it } from 'vitest'
import { formatMoney } from './money'

describe('formatMoney', () => {
  it('formats cents as CNY', () => {
    expect(formatMoney(9900).replaceAll('\u00a0', ' ')).toContain('99.00')
  })
})
