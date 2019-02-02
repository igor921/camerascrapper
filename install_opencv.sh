#!/usr/bin/env bash
#sudo apt install build-essential cmake git pkg-config libgtk-3-dev
#sudo apt install libavcodec-dev libavformat-dev libswscale-dev libv4l-dev libxvidcore-dev libx264-dev
#sudo apt install libjpeg-dev libpng-dev libtiff-dev gfortran openexr
#sudo apt install python3-dev python3-numpy libtbb2 libtbb-dev libdc1394-22-dev

mkdir ~/opencv_build && cd ~/opencv_build
git clone https://github.com/opencv/opencv.git
git clone https://github.com/opencv/opencv_contrib.git

cd ~/opencv_build/opencv
mkdir build && cd build
cmake -DWITH_LIBV4L=ON ..
make
make install