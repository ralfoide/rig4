
#![allow(non_snake_case)]

use shaku::{Component, Interface};

pub trait IFlags: Interface {
    fn getBlobDir(&self) -> &String;
}

#[derive(Component)]
#[shaku(interface = IFlags)]
pub struct Flags {
    mBlobDir: String
}

impl IFlags for Flags {
    fn getBlobDir(&self) -> &String {
        return &self.mBlobDir;
    }
}


#[cfg(test)]
mod tests_file_ops {
    use super::*;
    use shaku::{module, HasComponent};

    module! {
        TestModule {
            components = [Flags],
            providers = []
        }
    }

    #[test]
    fn test_getBlogDir() {
        let m = TestModule::builder()
            .with_component_parameters::<Flags>(FlagsParameters {
                mBlobDir: "/tmp/test".to_string()
            })
            .build();

        let f: &dyn IFlags = m.resolve_ref();
        assert_eq!(f.getBlobDir(), "/tmp/test");
    }
}
