
#![allow(non_snake_case)]

use shaku::{Component, Interface};
use std::sync::Arc;
use crate::rig4r::config::IFlags;

pub trait IBlobStore: Interface {
    fn putString(&mut self, description: &str, content: &str);
    fn getString(&self, description: &str) -> Option<&String>;
}

#[derive(Component)]
#[shaku(interface = IBlobStore)]
pub struct BlobStore {
    #[shaku(inject)]
    mFlags: Arc<dyn IFlags>
}

impl IBlobStore for BlobStore {
    fn putString(&mut self, description: &str, _content: &str) {
        println!("blob store put {}", description);
        // self.mCache.insert(String::from(description), String::from(content));
    }

    fn getString(&self, description: &str) -> Option<&String> {
        println!("blob store get {}", description);
        //self.mCache.get(description)
        Some(self.mFlags.getBlobDir())
    }
}

#[cfg(test)]
mod tests_blob_store {
    use shaku::{module, HasComponent};
    use crate::rig4r::config::*;
    use crate::rig4r::storage::*;

    module! {
        TestModule {
            components = [BlobStore, Flags],
            providers = []
        }
    }

    #[test]
    fn test_blob_store_module() {
        let mut m = TestModule::builder()
        .with_component_parameters::<Flags>(FlagsParameters {
            mBlobDir: "/tmp/test/blob".to_string()
        }).build();
        let hs: &mut dyn IBlobStore = m.resolve_mut().unwrap();
        assert_eq!(hs.getString("key").unwrap(), "/tmp/test/blob");

        hs.putString("key", "value");
        assert_eq!(hs.getString("key").unwrap(), "/tmp/test/blob");
        assert_eq!(hs.getString("key").unwrap(), "/tmp/test/blob");

        hs.putString("key", "value2");
        assert_eq!(hs.getString("key").unwrap(), "/tmp/test/blob");
    }
}
