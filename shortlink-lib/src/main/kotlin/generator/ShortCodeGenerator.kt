package generator

import model.ShortCode

interface ShortCodeGenerator {
  fun generate(): ShortCode
}