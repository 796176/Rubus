#!/bin/bash

if [[ ! -e $RUBUS_WORKING_DIR/rubus.conf ]]; then
  cp rubus.conf $RUBUS_WORKING_DIR/
fi