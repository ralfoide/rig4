package utils

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "os/user"
    "path/filepath"
)

func TestExpandUserPath(t *testing.T) {
    assert := assert.New(t)

    assert.Equal("", ExpandUserPath(""))
    assert.Equal("/foo", ExpandUserPath("/foo"))
    assert.Equal("/foo~/", ExpandUserPath("/foo~/"))

    u, _ := user.Current()
    assert.Equal(u.HomeDir + string(filepath.Separator) + "foo", ExpandUserPath("~/foo"))
}
