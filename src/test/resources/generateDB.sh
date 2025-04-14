#!/bin/bash

projectDir=`echo -n ~/IdeaProjects/Rubus`
data1_dir="$projectDir/src/test/resources/data1"
data2_dir="$projectDir/src/test/resources/data2"
rec1="abTitle11854480null1null2null3null4$data1_dir"
rec2="cdTitle221280720null1null2null3null4$data2_dir"

echo $rec1 >> "$projectDir/src/test/resources/testDB"
echo $rec2 >> "$projectDir/src/test/resources/testDB"
