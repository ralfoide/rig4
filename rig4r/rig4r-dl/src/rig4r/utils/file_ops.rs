
#![allow(non_snake_case)]

use shaku::{Component, Interface};
use std::collections::HashMap;

pub trait IFileOps: Interface {
    fn isFile(&self, path: &str) -> bool;
    fn createParentDirs(&mut self, path: &str);
    fn writeBytes(&mut self, bytes: &[u8], path: &str);
    fn readBytes(&self, path: &str) -> Vec<u8>;
}

#[derive(Component)]
#[shaku(interface = IFileOps)]
pub struct FileOps {
}

impl IFileOps for FileOps {
    fn isFile(&self, path: &str) -> bool {
        println!("real fileops.isFile {}", path);
        true
    }

    fn createParentDirs(&mut self, path: &str) {
        println!("real fileops.createParentDirs {}", path);
    }

    fn writeBytes(&mut self, _bytes: &[u8], path: &str) {
        println!("real fileops.writeBytes {}", path);
    }

    fn readBytes(&self, path: &str) -> Vec<u8> {
        println!("real fileops.readBytes {}", path);
        [1, 2, 3, 4].to_vec()
    }
}


#[derive(Component)]
#[shaku(interface = IFileOps)]
pub struct FakeFileOps {
    mEntries: HashMap<String, FakeData>
}

pub struct FakeData {
    mIsFile: bool,
    mData: Option<Vec<u8>>
}

impl IFileOps for FakeFileOps {
    fn isFile(&self, path: &str) -> bool {
        match self.mEntries.get(path) {
            Some(fakeData) => fakeData.mIsFile,
            None => false
        }
    }

    fn createParentDirs(&mut self, path: &str) {
        self.mEntries.insert(path.to_string(), FakeData{
            mIsFile: false,
            mData: None
        });
    }

    fn writeBytes(&mut self, bytes: &[u8], path: &str) {
        let v = bytes.to_vec();
        self.mEntries.insert(path.to_string(), FakeData{
            mIsFile: true,
            mData: Some(v)
        });
    }

    fn readBytes(&self, path: &str) -> Vec<u8> {
        match self.mEntries.get(path) {
            Some(fakeData) => fakeData.mData.as_ref().unwrap().to_vec(),
            None => Vec::new()
        }
    }
}

#[cfg(test)]
mod tests_file_ops {
    use shaku::{module, HasComponent};
    use crate::rig4r::utils::*;

    module! {
        TestModule {
            components = [FakeFileOps],
            providers = []
        }
    }

    #[test]
    fn test_file_ops_module() {
        let mut m = TestModule::builder().build();
        let f: &mut dyn IFileOps = m.resolve_mut().unwrap();

        assert_eq!(f.isFile("/fake/dir"), false);
        assert_eq!(f.isFile("/fake/file"), false);

        f.createParentDirs("/fake/dir");
        assert_eq!(f.isFile("/fake/dir"), false);

        f.writeBytes(&[1, 2, 3, 4], "/fake/file");
        assert_eq!(f.isFile("/fake/file"), true);
        assert_eq!(f.readBytes("/fake/file"), [1, 2, 3, 4]);
    }
}
