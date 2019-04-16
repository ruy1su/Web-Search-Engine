#!/usr/bin/env python3

import sys
import os
import numpy
import numpy.linalg
import scipy.misc

def getOutputPngName(path, rank):
    filename, ext = os.path.splitext(path)
    return filename + '.' + str(rank) + '.png'

def getOutputNpyName(path, rank):
    filename, ext = os.path.splitext(path)
    return filename + '.' + str(rank) + '.npy'

def saveFile(outputpng, outputnpy, m):
	numpy.save(outputnpy, m)
	scipy.misc.imsave(outputpng, m)

if len(sys.argv) < 3:
    sys.exit('usage: task1.py <PNG inputFile> <rank>')

inputfile = sys.argv[1]
rank = int(sys.argv[2])
outputpng = getOutputPngName(inputfile, rank)
outputnpy = getOutputNpyName(inputfile, rank)

#
# TODO: The current code just prints out what it is supposed to to
#       Replace the print statement wth your code
#
# print("This program should read %s file, perform rank %d approximation, and save the results in %s and %s files." % (inputfile, rank, outputpng, outputnpy))
im = scipy.misc.imread(inputfile)
u, m, v = numpy.linalg.svd(im)
uk, singular_diagonal, vk = u[:, :rank], numpy.diag(m[:rank]), v[:rank, :]
saveFile(outputpng, outputnpy, numpy.dot(uk, numpy.dot(singular_diagonal, vk)))
