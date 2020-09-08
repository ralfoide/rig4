
#![allow(non_snake_case)]

use shaku::{Component, Interface};
use std::collections::HashMap;
use std::sync::Arc;
use crate::rig4r::storage::IBlobStore;

pub trait IHashStore: Interface {
    fn putString(&mut self, description: &str, content: &str);
    fn getString(&self, description: &str) -> Option<&String>;
}

#[derive(Component)]
#[shaku(interface = IHashStore)]
pub struct HashStore {
    #[shaku(inject)]
    mBlobStore: Arc<dyn IBlobStore>,
    mCache: HashMap<String, String>
}

impl IHashStore for HashStore {
    fn putString(&mut self, description: &str, content: &str) {
        //self.mBlobStore.putString(description, content);
        self.mCache.insert(String::from(description), String::from(content));
    }

    fn getString(&self, description: &str) -> Option<&String> {
        self.mCache
            .get(description)
            .or_else(|| { self.mBlobStore.getString(description) })
    }
}

#[cfg(test)]
mod tests_hash_store {
    use shaku::{module, HasComponent};
    use crate::rig4r::config::*;
    use crate::rig4r::storage::*;

    module! {
        TestModule {
            components = [HashStore, BlobStore, Flags],
            providers = []
        }
    }

    #[test]
    fn test_hash_store_module() {
        let mut m = TestModule::builder()
            .with_component_parameters::<Flags>(FlagsParameters {
                mBlobDir: "/tmp/test/hash".to_string()
            }).build();
        let hs: &mut dyn IHashStore = m.resolve_mut().unwrap();
        assert_eq!(hs.getString("key").unwrap(), "/tmp/test/hash");

        hs.putString("key", "value");
        assert_eq!(hs.getString("key").unwrap(), "value");
        assert_eq!(hs.getString("key").unwrap(), "value");

        hs.putString("key", "value2");
        assert_eq!(hs.getString("key").unwrap(), "value2");
    }
}
