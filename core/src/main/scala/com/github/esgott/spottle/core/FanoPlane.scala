package com.github.esgott.spottle.core


import breeze.linalg.{sum, DenseMatrix, DenseVector}

import scala.math.abs


object FanoPlane:

  /** Generate an incidence matrix of a Fano Plane with the algorithm described in 1.2 of
    * https://arxiv.org/pdf/1603.05333.pdf
    */
  def apply(order: Int): DenseMatrix[Short] =
    val m = DenseMatrix.zeros[Short](size(order), size(order))

    for
      col <- 0 until size(order)
      row <- 0 until size(order)

      _ = if (m(row, col) == 0)
        m.update(row, col, 1)
        if (!check(m, order)) m.update(row, col, 0)
    yield ()

    m


  def size(order: Int): Int =
    order * order + order + 1


  private def check(m: DenseMatrix[Short], order: Int): Boolean =
    val rowSums = for row <- 0 until size(order) yield sum(m.row(row))
    val colSums = for col <- 0 until size(order) yield sum(m.col(col))

    val rowDiffs = for
      row1 <- 0 until size(order)
      row2 <- (row1 + 1) until size(order)
    yield vectorDiff(m.row(row1), m.row(row2))

    val colDiffs = for
      col1 <- 0 until size(order)
      col2 <- (col1 + 1) until size(order)
    yield vectorDiff(m.col(col1), m.col(col2))

    val rowSumsOk  = rowSums.forall(_ <= order + 1)
    val colSumsOk  = colSums.forall(_ <= order + 1)
    val rowDiffsOk = rowDiffs.forall(_ <= 1)
    val colDiffsOk = colDiffs.forall(_ <= 1)

    rowSumsOk && colSumsOk && rowDiffsOk && colDiffsOk


  private def vectorDiff(v1: DenseVector[Short], v2: DenseVector[Short]) =
    sum {
      (v1 + v2).map {
        case 2 => 1
        case 1 => 0
        case 0 => 0
      }
    }


  extension [T](m: DenseMatrix[T])
    def row(r: Int): DenseVector[T] = m(::, r)
    def col(c: Int): DenseVector[T] = m(c, ::).t
