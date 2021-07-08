package com.github.esgott.spottle.core


import breeze.linalg.DenseMatrix
import weaver._


object FanoPlaneTest extends SimpleIOSuite:

  pureTest("incident matrix order=2") {
    val result = FanoPlane(2)

    val expected = DenseMatrix(
      (1, 1, 1, 0, 0, 0, 0),
      (1, 0, 0, 1, 1, 0, 0),
      (1, 0, 0, 0, 0, 1, 1),
      (0, 1, 0, 1, 0, 1, 0),
      (0, 1, 0, 0, 1, 0, 1),
      (0, 0, 1, 1, 0, 0, 1),
      (0, 0, 1, 0, 1, 1, 0)
    )

    expect(
      result == expected
    )
  }
