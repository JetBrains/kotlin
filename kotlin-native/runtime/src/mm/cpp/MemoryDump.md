# Kotlin/Native dump file format v1.0.8 (draft).

## Specification

*   File extension is `.kdump`
*   The format of the file is binary
*   The file consists of *header* followed by a sequence of *blocks*
*   Each block starts with a *tag* byte, followed by a sequence of bytes
    corresponding to its tag

### Types

*   `u8` - unsigned 8-bit integer
*   `u32` - unsinged 32-bit integer with endianness specified in the header
*   `str` - null-terminated UTF-8 string
*   `id` - object ID with endianness and size specified in the header, where 0
    represents `null`
*   `runtime_type` - runtime type represented as `u8` value:
    * `1` = `OBJECT`
    * `2` = `INT_8`
    * `3` = `INT_16`
    * `4` = `INT_32`
    * `5` = `INT_64`
    * `6` = `FLOAT_32`
    * `7` = `FLOAT_64`
    * `8` = `NATIVE_PTR`
    * `9` = `BOOLEAN`
    * `10` = `VECTOR_128`

### Header

```
- header_string (str) = `Kotlin/Native dump 1.0.8`
- endianness (u8): 0 = big endian, 1 = little endian
- id_size (u8): 1, 2, 4 or 8 (usually 8)
```

### Blocks

#### TYPE

```
- tag (u8) = 1
- id (id): type ID
- flags (u8):
  - bit 0: 0 = object type, 1 = array type
  - bit 1: 0 = no debug_info, 1 = has debug_info
  - bit 2: 1 = object array, 0 = primitive array or object
  - bits 3..7: unused, should be zero
- super_type_id (id): super type ID, or 0 for root type
- package_name (str): name of the package, ie: "kotlin.collections"
- class_name (str): name of the class, ie: "Map$Entry"
- type body
  - for array type
    - element_size (u32): array element size in bytes
    - debug_info (optional):
      - element_type (u8): array element runtime type  
  - for object type
    - instance_size (u32): instance size in bytes
    - debug_info (optional):
      - field_count (u32): number of fields (0 for array)
      - list of fields, each with the given structure:
        - offset (u32): field offset
        - runtime_type (u8): field runtime type 
        - name (str): field name
```

#### OBJECT

```
- tag (u8) = 2
- id (id): object ID
- type_id (id): type ID
- size (u32): object size in bytes
- object_data: object data of the given size containing field values
```

#### ARRAY

```
- tag (u8) = 3
- id (id): array ID
- type_id (id): array type ID
- element_size (u32): size of array element
- count (u32): number of array elements
- element_data: array elements data of `element_size * count` size
```

#### EXTRA_OBJECT

```
- tag (u8) = 4
- id (id): extra object ID
- base_object_id (id): base object ID
- associated_object_id (id): associated object ID (used for ObjC interop)
```

#### THREAD

```
- tag (u8) = 5
- id (id): thread ID
```

#### GLOBAL_ROOT

```
- tag (u8) = 6
- source (u8): 1 = global, 2 = stable ref
- object_id (id): object ID
```

#### THREAD_ROOT

```
- tag (u8) = 7
- source (u8): 1 = stack, 2 = thread local
- thread_id (id): thread ID
- object_id (id): object ID
```
