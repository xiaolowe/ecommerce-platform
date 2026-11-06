import { describe, expect, it } from 'vitest'
import { formatMoney } from './money'

describe('formatMoney', () => {
  it('formats cents as CNY', () => {
    expect(formatMoney(1999).replaceAll('\u00a0', ' ')).toContain('19.99')
  })
})
